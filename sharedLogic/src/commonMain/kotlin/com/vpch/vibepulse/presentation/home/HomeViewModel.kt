package com.vpch.vibepulse.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpch.vibepulse.domain.coordinator.ActivityCoordinator
import com.vpch.vibepulse.domain.model.ActivityTrack
import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.repository.LocalUserRepository
import com.vpch.vibepulse.domain.repository.PlaybackRepository
import com.vpch.vibepulse.domain.repository.PreferencesRepository
import com.vpch.vibepulse.domain.repository.TrackRepository
import com.vpch.vibepulse.domain.repository.UserRepository
import com.vpch.vibepulse.domain.repository.WeatherRepository
import com.vpch.vibepulse.domain.usecase.LogoutUseCase
import com.vpch.vibepulse.domain.usecase.SaveActivityUseCase
import com.vpch.vibepulse.domain.usecase.ToggleTrackLikeUseCase
import com.vpch.vibepulse.location.LocationDisabledException
import com.vpch.vibepulse.location.LocationProvider
import com.vpch.vibepulse.service.RunTrackerNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(
    private val userRepository: UserRepository,
    private val localUserRepository: LocalUserRepository,
    private val weatherRepository: WeatherRepository,
    private val trackRepository: TrackRepository,
    private val playbackRepository: PlaybackRepository,
    private val preferencesRepository: PreferencesRepository,
    private val activityCoordinator: ActivityCoordinator,
    private val locationProvider: LocationProvider,
    private val runTrackerNotifier: RunTrackerNotifier,
    private val toggleTrackLike: ToggleTrackLikeUseCase,
    private val saveActivityUseCase: SaveActivityUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private companion object {
        const val MIN_ACTIVITY_DURATION_SECONDS = 60L
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var timerJob: Job? = null
    private class TrackSessionAccumulator(
        val track: Track,
        val cadenceSamples: MutableList<Int> = mutableListOf()
    )

    private val sessionPlayedTracks = linkedMapOf<String, TrackSessionAccumulator>()
    private var playerProgressJob: Job? = null
    private var lastCheckedLikedUri: String? = null
    private var lastSyncedControllerPosMs: Long = -1L

    init {
        observePlayback()
        observeSession()
        observeWeather()
        onIntent(HomeIntent.LoadInitialData)

        viewModelScope.launch {
            runTrackerNotifier.finishRequests.collect { finishRun() }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.LoadInitialData -> loadInitialData()
            HomeIntent.LogoutClicked -> logoutAndRedirect()
            is HomeIntent.LocationPermissionUpdated -> handlePermissionUpdate(intent.granted)
            HomeIntent.RefreshWeather -> loadWeather(forceRefresh = true)

            HomeIntent.StartRunClicked -> startRun()
            HomeIntent.PauseRunClicked -> pauseRun()
            HomeIntent.ResumeRunClicked -> resumeRun()
            HomeIntent.FinishRunClicked -> finishRun()

            HomeIntent.TogglePlayPauseClicked -> togglePlayPause()
            HomeIntent.SkipNextClicked -> skipToNext()
            is HomeIntent.SeekTo -> seekTo(intent.positionMs)
            HomeIntent.ToggleLikeClicked -> toggleLike()
        }
    }

    private fun observePlayback() {
        viewModelScope.launch {
            playbackRepository.state.collect { snapshot ->
                val trackChanged = snapshot.currentTrack?.spotifyUri != lastCheckedLikedUri
                if (snapshot.positionMs != lastSyncedControllerPosMs) {
                    lastSyncedControllerPosMs = snapshot.positionMs
                }
                val currentSuccess = _uiState.value as? HomeUiState.Success
                if (currentSuccess?.run?.status == RunStatus.RUNNING) {
                    val activeTrack = snapshot.currentTrack ?: currentSuccess.player.currentTrack
                    if (activeTrack != null && activeTrack.spotifyUri.isNotBlank()) {
                        sessionPlayedTracks.getOrPut(activeTrack.spotifyUri) {
                            TrackSessionAccumulator(activeTrack)
                        }
                    }
                }
                updateSuccessState { state ->
                    val resolvedTrack = snapshot.currentTrack ?: state.player.currentTrack
                    val duration = snapshot.durationMs.takeIf { it > 0L }
                        ?: resolvedTrack?.durationMs
                        ?: 0L

                    state.copy(
                        player = state.player.copy(
                            currentTrack = resolvedTrack,
                            isPlaying = snapshot.isPlaying,
                            positionMs = snapshot.positionMs,
                            durationMs = duration
                        )
                    )
                }

                ensurePlayerProgressLoop(snapshot.isPlaying)
                if (trackChanged) {
                    syncTrackLikedStatus(snapshot.currentTrack)
                }
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            activityCoordinator.sessionState.collect { session ->
                updateSuccessState { state ->
                    state.copy(
                        run = state.run.copy(
                            currentCadence = session.currentCadence,
                            cadenceZone = session.cadenceZone
                        )
                    )
                }
            }
        }
    }

    private fun observeWeather() {
        viewModelScope.launch {
            weatherRepository.currentWeather.collect { snapshot ->
                if (snapshot != null) {
                    updateSuccessState { state ->
                        state.copy(weather = state.weather.copy(snapshot = snapshot))
                    }
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            userRepository.getCurrentUser()
                .onSuccess { user ->
                    localUserRepository.saveUser(user)
                    val hasPermission = locationProvider.hasLocationPermission()
                    _uiState.value = HomeUiState.Success(
                        user = user,
                        weather = WeatherUiState(
                            snapshot = weatherRepository.currentWeather.value,
                            hasPermission = hasPermission,
                            isLoading = hasPermission
                        )
                    )

                    connectAndSyncPlayback()

                    if (hasPermission) {
                        loadWeather(forceRefresh = false)
                    } else {
                        _effects.send(HomeEffect.RequestLocationPermission)
                    }
                }
                .onFailure { error ->
                    if (isUnauthorizedError(error)) {
                        handleSessionExpired()
                    } else {
                        val msg = error.message?.takeIf { it.isNotBlank() }
                            ?.let { HomeMessage.Custom(it) }
                            ?: HomeMessage.ProfileLoadError
                        _uiState.value = HomeUiState.Error(message = msg)
                    }
                }
        }
    }

    private fun connectAndSyncPlayback() {
        viewModelScope.launch {
            val connected = playbackRepository.connectRemote().isSuccess
            if (!connected || playbackRepository.state.value.currentTrack == null) {
                playbackRepository.syncState()
            }
        }
    }

    private fun logoutAndRedirect() {
        viewModelScope.launch {
            timerJob?.cancel()
            playerProgressJob?.cancel()
            logoutUseCase()
            _effects.send(HomeEffect.NavigateToAuth)
        }
    }

    private suspend fun handleSessionExpired() {
        logoutUseCase()
        _effects.send(HomeEffect.NavigateToAuth)
    }

    private fun isUnauthorizedError(error: Throwable): Boolean {
        val msg = error.message.orEmpty()
        return msg.contains("401") ||
                msg.contains("Session expired", ignoreCase = true) ||
                msg.contains("Сесія Spotify застаріла", ignoreCase = true)
    }

    private fun handlePermissionUpdate(granted: Boolean) {
        updateSuccessState { it.copy(weather = it.weather.copy(hasPermission = granted)) }
        if (granted) {
            loadWeather(forceRefresh = true)
        } else {
            updateSuccessState {
                it.copy(
                    weather = it.weather.copy(
                        isLoading = false,
                        error = HomeMessage.LocationPermissionRequired
                    )
                )
            }
        }
    }

    private fun loadWeather(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentWeather = (_uiState.value as? HomeUiState.Success)?.weather?.snapshot
            updateSuccessState {
                it.copy(
                    weather = it.weather.copy(
                        isLoading = forceRefresh || currentWeather == null,
                        error = null
                    )
                )
            }

            val result = kotlinx.coroutines.withTimeoutOrNull(14_000L) {
                weatherRepository.getWeather(forceRefresh = forceRefresh)
            }

            if (result == null) {
                val timeoutMsg = HomeMessage.WeatherLoadError
                updateSuccessState {
                    it.copy(
                        weather = it.weather.copy(
                            isLoading = false,
                            error = timeoutMsg
                        )
                    )
                }
                if (forceRefresh || currentWeather == null) {
                    _effects.send(HomeEffect.ShowMessage(timeoutMsg))
                }
                return@launch
            }

            result
                .onSuccess { snapshot ->
                    updateSuccessState { state ->
                        state.copy(
                            weather = state.weather.copy(
                                snapshot = snapshot,
                                isLoading = false,
                                isLocationEnabled = locationProvider.isLocationEnabled(),
                                error = null
                            )
                        )
                    }
                }
                .onFailure { error ->
                    val isGpsOff = error is LocationDisabledException || !locationProvider.isLocationEnabled()
                    val msg = if (isGpsOff) {
                        HomeMessage.GpsDisabled
                    } else {
                        error.message?.takeIf { it.isNotBlank() }
                            ?.let { HomeMessage.Custom(it) }
                            ?: HomeMessage.WeatherLoadError
                    }

                    updateSuccessState {
                        it.copy(
                            weather = it.weather.copy(
                                isLoading = false,
                                isLocationEnabled = !isGpsOff,
                                error = msg
                            )
                        )
                    }

                    if (forceRefresh || currentWeather == null || isGpsOff) {
                        _effects.send(HomeEffect.ShowMessage(msg))
                        if (forceRefresh && isGpsOff) {
                            _effects.send(HomeEffect.OpenLocationSettings)
                        }
                    }
                }
        }
    }
    private fun startRun() {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val user = currentState.user ?: User(
            spotifyId = "",
            displayName = "Runner",
            hasPremium = false
        )

        sessionPlayedTracks.clear()
        currentState.player.currentTrack?.let { track ->
            sessionPlayedTracks.getOrPut(track.spotifyUri) { TrackSessionAccumulator(track) }
        }
        activityCoordinator.startSession(user)
        runTrackerNotifier.onRunStarted()

        updateSuccessState { state ->
            state.copy(
                run = RunUiState(status = RunStatus.RUNNING)
            )
        }
        startTimer()

        viewModelScope.launch {
            activityCoordinator.startInitialPlayback(
                weather = currentState.weather.snapshot,
                preferences = preferencesRepository.preferences.value
            )
        }
    }

    private fun pauseRun() {
        timerJob?.cancel()
        ensurePlayerProgressLoop(false)
        activityCoordinator.pauseSession()
        playbackRepository.pauseImmediate()

        val runState = (_uiState.value as? HomeUiState.Success)?.run
        runTrackerNotifier.onRunUpdated(
            cadence = runState?.currentCadence ?: 0,
            elapsedSeconds = runState?.elapsedSeconds ?: 0L,
            isPaused = true
        )
        updateSuccessState {
            it.copy(
                run = it.run.copy(status = RunStatus.PAUSED),
                player = it.player.copy(isPlaying = false)
            )
        }

        viewModelScope.launch {
            playbackRepository.pause()
        }
    }

    private fun resumeRun() {
        activityCoordinator.resumeSession()
        val runState = (_uiState.value as? HomeUiState.Success)?.run
        runTrackerNotifier.onRunUpdated(
            cadence = runState?.currentCadence ?: 0,
            elapsedSeconds = runState?.elapsedSeconds ?: 0L,
            isPaused = false
        )
        updateSuccessState {
            it.copy(
                run = it.run.copy(status = RunStatus.RUNNING),
                player = it.player.copy(isPlaying = true)
            )
        }
        ensurePlayerProgressLoop(true)
        startTimer()

        viewModelScope.launch {
            playbackRepository.resume()
        }
    }

    private fun finishRun() {
        timerJob?.cancel()
        runTrackerNotifier.onRunStopped()
        playbackRepository.pauseImmediate()

        val currentState = _uiState.value as? HomeUiState.Success
        val elapsedSeconds = currentState?.run?.elapsedSeconds ?: 0L
        val currentUser = currentState?.user

        val completedActivity = activityCoordinator.endSession()
        val recordedTracks = sessionPlayedTracks.values
            .toList()
            .asReversed()
            .map { acc ->
                val avgTrackCadence = if (acc.cadenceSamples.isNotEmpty()) {
                    acc.cadenceSamples.average().toInt()
                } else {
                    0
                }
                ActivityTrack(
                    spotifyUri = acc.track.spotifyUri,
                    title = acc.track.title,
                    artist = acc.track.artist,
                    albumArtUrl = acc.track.albumArtUrl,
                    tempo = acc.track.tempo,
                    avgCadenceDuringTrack = avgTrackCadence
                )
            }
        sessionPlayedTracks.clear()

        updateSuccessState {
            it.copy(
                run = RunUiState(),
                player = PlayerUiState()
            )
        }

        viewModelScope.launch {
            playbackRepository.pause()

            if (completedActivity != null && currentUser != null) {
                if (elapsedSeconds >= MIN_ACTIVITY_DURATION_SECONDS) {
                    saveActivityUseCase(
                        user = currentUser,
                        activity = completedActivity,
                        activeDurationSeconds = elapsedSeconds,
                        tracks = recordedTracks
                    ).onSuccess {
                        _effects.send(HomeEffect.ShowMessage(HomeMessage.ActivitySaved))
                    }.onFailure {
                        _effects.send(HomeEffect.ShowMessage(HomeMessage.ActivitySaveFailed))
                    }
                } else {
                    _effects.send(
                        HomeEffect.ShowMessage(
                            HomeMessage.ActivityTooShort(MIN_ACTIVITY_DURATION_SECONDS)
                        )
                    )
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000.milliseconds)
                activityCoordinator.onTick(
                    preferences = preferencesRepository.preferences.value
                )

                val currentSuccess = _uiState.value as? HomeUiState.Success
                val activeTrack = currentSuccess?.player?.currentTrack
                val currentCadence = activityCoordinator.sessionState.value.currentCadence
                if (activeTrack != null && activeTrack.spotifyUri.isNotBlank() && currentCadence > 0) {
                    sessionPlayedTracks
                        .getOrPut(activeTrack.spotifyUri) { TrackSessionAccumulator(activeTrack) }
                        .cadenceSamples
                        .add(currentCadence)
                }

                val sessionSnapshot = activityCoordinator.sessionState.value
                if (!sessionSnapshot.isAutoPaused) {
                    updateSuccessState { state ->
                        state.copy(
                            run = state.run.copy(elapsedSeconds = state.run.elapsedSeconds + 1)
                        )
                    }
                }

                val updated = (_uiState.value as? HomeUiState.Success)?.run
                if (updated != null && updated.status == RunStatus.RUNNING) {
                    runTrackerNotifier.onRunUpdated(
                        cadence = updated.currentCadence,
                        elapsedSeconds = updated.elapsedSeconds,
                        isPaused = false
                    )
                }
            }
        }
    }

    private fun ensurePlayerProgressLoop(isPlaying: Boolean) {
        if (!isPlaying) {
            playerProgressJob?.cancel()
            playerProgressJob = null
            return
        }
        if (playerProgressJob?.isActive == true) return

        playerProgressJob = viewModelScope.launch {
            var secondsSinceLastSync = 0
            while (isActive) {
                delay(1000.milliseconds)
                val currentState = _uiState.value as? HomeUiState.Success ?: break
                if (!currentState.player.isPlaying) break

                secondsSinceLastSync++
                val playback = playbackRepository.state.value
                val syncInterval = if (playback.isRemoteConnected) 1 else 3

                if (currentState.run.status != RunStatus.RUNNING && secondsSinceLastSync >= syncInterval) {
                    secondsSinceLastSync = 0
                    playbackRepository.syncState()
                }

                val duration = currentState.player.durationMs
                val nextPosition = if (duration > 0L) {
                    (currentState.player.positionMs + 1000L).coerceAtMost(duration)
                } else {
                    currentState.player.positionMs + 1000L
                }

                updateSuccessState { state ->
                    state.copy(player = state.player.copy(positionMs = nextPosition))
                }
            }
        }
    }

    private fun togglePlayPause() {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val currentlyPlaying = currentState.player.isPlaying
        val targetPlaying = !currentlyPlaying

        updateSuccessState { it.copy(player = it.player.copy(isPlaying = targetPlaying)) }
        ensurePlayerProgressLoop(targetPlaying)

        viewModelScope.launch {
            val result = if (currentlyPlaying) {
                playbackRepository.pause()
            } else {
                playbackRepository.resume()
            }

            result.onFailure { error ->
                updateSuccessState { it.copy(player = it.player.copy(isPlaying = currentlyPlaying)) }
                ensurePlayerProgressLoop(currentlyPlaying)
                handleActionError(error, HomeMessage.PlayerControlError)
            }
        }
    }

    private fun skipToNext() {
        viewModelScope.launch {
            playbackRepository.skipToNext()
                .onSuccess {
                    if ((_uiState.value as? HomeUiState.Success)?.run?.status == RunStatus.RUNNING) {
                        activityCoordinator.ensureQueuePopulated(preferencesRepository.preferences.value)
                    }
                }
                .onFailure { error ->
                    handleActionError(error, HomeMessage.SkipTrackError)
                }
        }
    }

    private fun seekTo(positionMs: Long) {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        if (currentState.player.currentTrack == null) return

        val duration = currentState.player.durationMs
        val clampedMs = if (duration > 0L) {
            positionMs.coerceIn(0L, duration)
        } else {
            positionMs.coerceAtLeast(0L)
        }

        lastSyncedControllerPosMs = clampedMs
        updateSuccessState { it.copy(player = it.player.copy(positionMs = clampedMs)) }

        viewModelScope.launch {
            playbackRepository.seekTo(clampedMs)
                .onFailure { error ->
                    handleActionError(error, HomeMessage.SeekTrackError)
                }
        }
    }

    private fun toggleLike() {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val track = currentState.player.currentTrack ?: return
        if (currentState.player.isLikeInProgress) return

        val targetLiked = !currentState.player.isLiked

        updateSuccessState {
            it.copy(
                player = it.player.copy(
                    isLiked = targetLiked,
                    isLikeInProgress = true
                )
            )
        }

        viewModelScope.launch {
            toggleTrackLike(track.spotifyUri, targetLiked)
                .onSuccess {
                    updateSuccessState {
                        it.copy(player = it.player.copy(isLikeInProgress = false))
                    }
                    _effects.send(
                        HomeEffect.ShowMessage(
                            if (targetLiked) HomeMessage.TrackLiked else HomeMessage.TrackUnliked
                        )
                    )
                }
                .onFailure { error ->
                    updateSuccessState {
                        it.copy(
                            player = it.player.copy(
                                isLiked = !targetLiked,
                                isLikeInProgress = false
                            )
                        )
                    }
                    handleActionError(error, HomeMessage.LikeUpdateError)
                }
        }
    }

    private fun syncTrackLikedStatus(track: Track?) {
        if (track == null) {
            lastCheckedLikedUri = null
            updateSuccessState { it.copy(player = it.player.copy(isLiked = false)) }
            return
        }
        if (track.spotifyUri == lastCheckedLikedUri) return

        lastCheckedLikedUri = track.spotifyUri
        updateSuccessState { it.copy(player = it.player.copy(isLiked = false)) }

        viewModelScope.launch {
            val trackId = track.spotifyUri.substringAfterLast(":")
            trackRepository.isTrackSaved(trackId).onSuccess { saved ->
                val currentUri = (_uiState.value as? HomeUiState.Success)?.player?.currentTrack?.spotifyUri
                if (currentUri == track.spotifyUri) {
                    updateSuccessState { it.copy(player = it.player.copy(isLiked = saved)) }
                }
            }
        }
    }

    private suspend fun handleActionError(error: Throwable, fallbackMessage: HomeMessage) {
        if (isUnauthorizedError(error)) {
            handleSessionExpired()
        } else {
            val resolved = error.message?.takeIf { it.isNotBlank() }
                ?.let { HomeMessage.Custom(it) }
                ?: fallbackMessage
            _effects.send(HomeEffect.ShowMessage(resolved))
        }
    }

    private inline fun updateSuccessState(
        crossinline transform: (HomeUiState.Success) -> HomeUiState.Success
    ) {
        _uiState.update { currentState ->
            if (currentState is HomeUiState.Success) {
                transform(currentState)
            } else {
                currentState
            }
        }
    }

    override fun onCleared() {
        playerProgressJob?.cancel()
        timerJob?.cancel()
        runTrackerNotifier.onRunStopped()
        playbackRepository.pauseImmediate()
        activityCoordinator.endSession()

        CoroutineScope(SupervisorJob() + Dispatchers.IO + NonCancellable).launch {
            playbackRepository.pause()
            playbackRepository.disconnectRemote()
        }
        super.onCleared()
    }
}