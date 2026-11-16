package com.vpch.vibepulse.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpch.vibepulse.domain.coordinator.ActivityCoordinator
import com.vpch.vibepulse.domain.model.Activity
import com.vpch.vibepulse.domain.model.CadenceZone
import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.location.LocationDisabledException
import com.vpch.vibepulse.location.LocationProvider
import com.vpch.vibepulse.network.SpotifyApiService
import com.vpch.vibepulse.service.RunTrackerNotifier
import com.vpch.vibepulse.storage.TokenStorage
import com.vpch.vibepulse.utils.toDomain
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

enum class RunStatus {
    IDLE,
    RUNNING,
    PAUSED
}

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val user: User? = null,
        val weather: WeatherSnapshot? = null,
        val activity: Activity? = null,
        val currentTrack: Track? = null,

        val isPlaying: Boolean = false,
        val isTrackLiked: Boolean = false,
        val isLikeInProgress: Boolean = false,
        val runStatus: RunStatus = RunStatus.IDLE,
        val currentCadence: Int = 0,
        val cadenceZone: CadenceZone = CadenceZone.IDLE,
        val elapsedSeconds: Long = 0L,
        val trackPositionMs: Long = 0L,
        val trackDurationMs: Long = 0L,

        val hasLocationPermission: Boolean = false,
        val isLocationEnabled: Boolean = true,
        val isWeatherLoading: Boolean = false,
        val weatherError: String? = null
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val spotifyApiService: SpotifyApiService,
    private val locationProvider: LocationProvider,
    private val activityCoordinator: ActivityCoordinator,
    private val tokenStorage: TokenStorage,
    private val runTrackerNotifier: RunTrackerNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var timerJob: Job? = null
    private var playerProgressJob: Job? = null
    private var lastCheckedLikedUri: String? = null
    private var lastSyncedControllerPosMs: Long = -1L

    init {
        onIntent(HomeIntent.LoadInitialData)

        viewModelScope.launch {
            runTrackerNotifier.finishRequests.collect {
                finishRun()
            }
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

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            spotifyApiService.getCurrentUser()
                .onSuccess { profileDto ->
                    val hasPermission = locationProvider.hasLocationPermission()
                    _uiState.value = HomeUiState.Success(
                        user = profileDto.toDomain(),
                        hasLocationPermission = hasPermission,
                        isWeatherLoading = hasPermission
                    )

                    loadPlaybackState()

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
                        _uiState.value = HomeUiState.Error(
                            message = error.message ?: "Не вдалося завантажити дані профілю"
                        )
                    }
                }
        }
    }

    private fun logoutAndRedirect() {
        viewModelScope.launch {
            timerJob?.cancel()
            playerProgressJob?.cancel()
            runTrackerNotifier.onRunStopped()
            activityCoordinator.playbackController.pauseImmediate()
            activityCoordinator.playbackController.pause()
            activityCoordinator.endSession()
            handleSessionExpired()
        }
    }

    private suspend fun handleSessionExpired() {
        tokenStorage.clearTokens()
        _effects.send(HomeEffect.NavigateToAuth)
    }

    private fun isUnauthorizedError(error: Throwable): Boolean {
        val msg = error.message.orEmpty()
        return msg.contains("401") ||
                msg.contains("Session expired", ignoreCase = true) ||
                msg.contains("Сесія Spotify застаріла", ignoreCase = true)
    }

    private fun handlePermissionUpdate(granted: Boolean) {
        updateSuccessState { it.copy(hasLocationPermission = granted) }
        if (granted) {
            loadWeather(forceRefresh = true)
        } else {
            updateSuccessState {
                it.copy(
                    isWeatherLoading = false,
                    weatherError = "Надайте доступ до локації для відображення погоди"
                )
            }
        }
    }

    private fun loadWeather(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentWeather = (_uiState.value as? HomeUiState.Success)?.weather
            updateSuccessState {
                it.copy(
                    isWeatherLoading = currentWeather == null,
                    weatherError = null
                )
            }

            activityCoordinator.weatherService.getWeather(forceRefresh = forceRefresh)
                .onSuccess { snapshot ->
                    updateSuccessState { state ->
                        state.copy(
                            isWeatherLoading = false,
                            isLocationEnabled = locationProvider.isLocationEnabled(),
                            weather = snapshot,
                            weatherError = null,
                            activity = state.activity?.attachWeather(snapshot)
                        )
                    }
                }
                .onFailure { error ->
                    val isGpsOff = error is LocationDisabledException || !locationProvider.isLocationEnabled()
                    val msg = if (isGpsOff) {
                        "Увімкніть геолокацію (GPS) для визначення погоди"
                    } else {
                        error.message ?: "Помилка завантаження погоди"
                    }

                    updateSuccessState {
                        it.copy(
                            isWeatherLoading = false,
                            isLocationEnabled = !isGpsOff,
                            weatherError = msg
                        )
                    }

                    if (currentWeather == null && (forceRefresh || isGpsOff)) {
                        _effects.send(HomeEffect.ShowMessage(msg))
                        if (forceRefresh && isGpsOff) {
                            _effects.send(HomeEffect.OpenLocationSettings)
                        }
                    }
                }
        }
    }

    private fun loadPlaybackState() {
        viewModelScope.launch {
            val connected = activityCoordinator.playbackController.connectRemote().isSuccess
            if (!connected || activityCoordinator.playbackController.currentTrack == null) {
                activityCoordinator.playbackController.syncPlaybackState()
            }
            syncPlayerUi()
        }
    }

    private fun startRun() {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val user = currentState.user ?: User(
            spotifyId = "",
            displayName = "Runner",
            hasPremium = false
        )

        val startedActivity = activityCoordinator.startSession(user)
        runTrackerNotifier.onRunStarted()

        updateSuccessState { state ->
            state.copy(
                activity = startedActivity,
                runStatus = RunStatus.RUNNING,
                elapsedSeconds = 0L,
                currentCadence = 0
            )
        }
        startTimer()

        viewModelScope.launch {
            activityCoordinator.startInitialPlayback(currentState.weather)
            syncPlayerUi()
        }
    }

    private fun pauseRun() {
        timerJob?.cancel()
        ensurePlayerProgressLoop(false)
        activityCoordinator.pauseSession()
        activityCoordinator.playbackController.pauseImmediate()

        val state = _uiState.value as? HomeUiState.Success
        runTrackerNotifier.onRunUpdated(
            cadence = state?.currentCadence ?: 0,
            elapsedSeconds = state?.elapsedSeconds ?: 0L,
            isPaused = true
        )
        updateSuccessState {
            it.copy(
                runStatus = RunStatus.PAUSED,
                isPlaying = false
            )
        }

        viewModelScope.launch {
            activityCoordinator.playbackController.pause()
            syncPlayerUi()
        }
    }

    private fun resumeRun() {
        activityCoordinator.resumeSession()
        val state = _uiState.value as? HomeUiState.Success
        runTrackerNotifier.onRunUpdated(
            cadence = state?.currentCadence ?: 0,
            elapsedSeconds = state?.elapsedSeconds ?: 0L,
            isPaused = false
        )
        updateSuccessState {
            it.copy(
                runStatus = RunStatus.RUNNING,
                isPlaying = true
            )
        }
        ensurePlayerProgressLoop(true)
        startTimer()

        viewModelScope.launch {
            activityCoordinator.playbackController.resume()
            syncPlayerUi()
        }
    }

    private fun finishRun() {
        timerJob?.cancel()
        runTrackerNotifier.onRunStopped()
        activityCoordinator.playbackController.pauseImmediate()
        val completedActivity = activityCoordinator.endSession()
        // TODO: Зберегти completedActivity у статистику

        updateSuccessState {
            it.copy(
                activity = null,
                runStatus = RunStatus.IDLE,
                elapsedSeconds = 0L,
                currentCadence = 0,
                cadenceZone = CadenceZone.IDLE,
                isPlaying = false
            )
        }

        viewModelScope.launch {
            activityCoordinator.playbackController.pause()
            syncPlayerUi()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000.milliseconds)
                activityCoordinator.onTick()

                val controller = activityCoordinator.playbackController
                updateSuccessState { state ->
                    val activeSession = activityCoordinator.activity
                    val spm = activeSession?.samples?.lastOrNull()?.stepsPerMinute ?: 0
                    state.copy(
                        elapsedSeconds = state.elapsedSeconds + 1,
                        activity = activeSession,
                        weather = activeSession?.weather ?: state.weather,
                        currentCadence = spm,
                        cadenceZone = CadenceZone.fromSpm(spm),
                        currentTrack = controller.currentTrack ?: state.currentTrack,
                        isPlaying = controller.isPlaying
                    )
                }

                ensurePlayerProgressLoop(controller.isPlaying)

                val updated = _uiState.value as? HomeUiState.Success
                if (updated != null) {
                    syncTrackLikedStatus(updated.currentTrack)
                    if (updated.runStatus == RunStatus.RUNNING) {
                        runTrackerNotifier.onRunUpdated(
                            cadence = updated.currentCadence,
                            elapsedSeconds = updated.elapsedSeconds,
                            isPaused = false
                        )
                    }
                }
            }
        }
    }

    private fun syncPlayerUi() {
        val controller = activityCoordinator.playbackController
        val activeTrack = controller.currentTrack
        val controllerPos = controller.trackPositionMs
        lastSyncedControllerPosMs = controllerPos

        updateSuccessState { state ->
            val resolvedTrack = activeTrack ?: state.currentTrack
            val duration = controller.trackDurationMs.takeIf { it > 0L }
                ?: resolvedTrack?.durationMs
                ?: 0L

            state.copy(
                currentTrack = resolvedTrack,
                isPlaying = controller.isPlaying,
                trackPositionMs = controllerPos,
                trackDurationMs = duration
            )
        }

        ensurePlayerProgressLoop(controller.isPlaying)

        val displayedTrack = (_uiState.value as? HomeUiState.Success)?.currentTrack
        syncTrackLikedStatus(displayedTrack)
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
                if (!currentState.isPlaying) break

                secondsSinceLastSync++
                val controller = activityCoordinator.playbackController
                val syncInterval = if (controller.isRemoteConnected) 1 else 3

                if (currentState.runStatus != RunStatus.RUNNING && secondsSinceLastSync >= syncInterval) {
                    secondsSinceLastSync = 0
                    controller.syncPlaybackState()
                }

                val activeTrack = controller.currentTrack ?: currentState.currentTrack
                val duration = controller.trackDurationMs.takeIf { it > 0L }
                    ?: activeTrack?.durationMs
                    ?: 0L

                val controllerPos = controller.trackPositionMs
                val nextPosition = when {
                    controllerPos != lastSyncedControllerPosMs -> {
                        lastSyncedControllerPosMs = controllerPos
                        controllerPos
                    }
                    duration > 0L -> (currentState.trackPositionMs + 1000L).coerceAtMost(duration)
                    else -> currentState.trackPositionMs + 1000L
                }

                updateSuccessState { state ->
                    state.copy(
                        currentTrack = activeTrack,
                        isPlaying = controller.isPlaying,
                        trackPositionMs = nextPosition,
                        trackDurationMs = duration
                    )
                }
                syncTrackLikedStatus(activeTrack)
            }
        }
    }

    private fun togglePlayPause() {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val currentlyPlaying = currentState.isPlaying
        val targetPlaying = !currentlyPlaying

        // Миттєво оновлюємо UI при натисканні
        updateSuccessState { it.copy(isPlaying = targetPlaying) }
        ensurePlayerProgressLoop(targetPlaying)

        viewModelScope.launch {
            val result = if (currentlyPlaying) {
                activityCoordinator.playbackController.pause()
            } else {
                activityCoordinator.playbackController.resume()
            }

            result
                .onSuccess {
                    syncPlayerUi()
                }
                .onFailure { error ->
                    updateSuccessState { it.copy(isPlaying = currentlyPlaying) }
                    ensurePlayerProgressLoop(currentlyPlaying)
                    if (isUnauthorizedError(error)) {
                        handleSessionExpired()
                    } else {
                        _effects.send(
                            HomeEffect.ShowMessage(error.message ?: "Помилка керування плеєром")
                        )
                    }
                }
        }
    }

    private fun skipToNext() {
        viewModelScope.launch {
            activityCoordinator.playbackController.skipToNext()
                .onSuccess {
                    syncPlayerUi()
                }
                .onFailure { error ->
                    if (isUnauthorizedError(error)) {
                        handleSessionExpired()
                    } else {
                        _effects.send(
                            HomeEffect.ShowMessage(error.message ?: "Не вдалося перемкнути трек")
                        )
                    }
                }
        }
    }

    private fun toggleLike() {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val track = currentState.currentTrack ?: return
        if (currentState.isLikeInProgress) return

        val targetLiked = !currentState.isTrackLiked
        val trackId = track.spotifyUri.substringAfterLast(":")

        updateSuccessState {
            it.copy(
                isTrackLiked = targetLiked,
                isLikeInProgress = true
            )
        }

        viewModelScope.launch {
            val result = if (targetLiked) {
                spotifyApiService.saveTrack(trackId)
            } else {
                spotifyApiService.removeSavedTrack(trackId)
            }

            result
                .onSuccess {
                    updateSuccessState { it.copy(isLikeInProgress = false) }
                    _effects.send(
                        HomeEffect.ShowMessage(
                            if (targetLiked) "Додано в улюблені" else "Видалено з улюблених"
                        )
                    )
                }
                .onFailure { error ->
                    updateSuccessState {
                        it.copy(
                            isTrackLiked = !targetLiked,
                            isLikeInProgress = false
                        )
                    }
                    if (isUnauthorizedError(error)) {
                        handleSessionExpired()
                    } else {
                        _effects.send(
                            HomeEffect.ShowMessage(error.message ?: "Не вдалося оновити улюблені")
                        )
                    }
                }
        }
    }


    private fun syncTrackLikedStatus(track: Track?) {
        if (track == null) {
            lastCheckedLikedUri = null
            updateSuccessState { it.copy(isTrackLiked = false) }
            return
        }
        if (track.spotifyUri == lastCheckedLikedUri) return

        lastCheckedLikedUri = track.spotifyUri
        updateSuccessState { it.copy(isTrackLiked = false) }

        viewModelScope.launch {
            val trackId = track.spotifyUri.substringAfterLast(":")
            spotifyApiService.isTrackSaved(trackId).onSuccess { saved ->
                val currentUri = (_uiState.value as? HomeUiState.Success)?.currentTrack?.spotifyUri
                if (currentUri == track.spotifyUri) {
                    updateSuccessState { it.copy(isTrackLiked = saved) }
                }
            }
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

    private fun seekTo(positionMs: Long) {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        if (currentState.currentTrack == null) return

        val clampedMs = if (currentState.trackDurationMs > 0L) {
            positionMs.coerceIn(0L, currentState.trackDurationMs)
        } else {
            positionMs.coerceAtLeast(0L)
        }

        lastSyncedControllerPosMs = clampedMs
        updateSuccessState { it.copy(trackPositionMs = clampedMs) }

        viewModelScope.launch {
            activityCoordinator.playbackController.seekTo(clampedMs)
                .onFailure { error ->
                    if (isUnauthorizedError(error)) {
                        handleSessionExpired()
                    } else {
                        _effects.send(
                            HomeEffect.ShowMessage(error.message ?: "Не вдалося перемотати трек")
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        playerProgressJob?.cancel()
        timerJob?.cancel()
        runTrackerNotifier.onRunStopped()
        activityCoordinator.playbackController.pauseImmediate()
        activityCoordinator.endSession()

        CoroutineScope(SupervisorJob() + Dispatchers.IO + NonCancellable).launch {
            activityCoordinator.playbackController.pause()
            activityCoordinator.playbackController.disconnectRemote()
        }
        super.onCleared()
    }
}