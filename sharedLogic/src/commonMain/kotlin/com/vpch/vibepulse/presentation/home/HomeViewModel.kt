package com.vpch.vibepulse.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpch.vibepulse.domain.coordinator.ActivityCoordinator
import com.vpch.vibepulse.domain.model.Activity
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
    private var lastCheckedLikedUri: String? = null

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
            activityCoordinator.playbackController.connectRemote()
            activityCoordinator.playbackController.syncPlaybackState()
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
        updateSuccessState { it.copy(runStatus = RunStatus.RUNNING) }
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

                updateSuccessState { state ->
                    val activeSession = activityCoordinator.activity
                    state.copy(
                        elapsedSeconds = state.elapsedSeconds + 1,
                        activity = activeSession,
                        weather = activeSession?.weather ?: state.weather,
                        currentCadence = activeSession?.samples?.lastOrNull()?.stepsPerMinute ?: 0,
                        currentTrack = activityCoordinator.playbackController.currentTrack ?: state.currentTrack,
                        isPlaying = activityCoordinator.playbackController.isPlaying
                    )
                }

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

    private fun togglePlayPause() {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val currentlyPlaying = currentState.isPlaying

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

    private fun syncPlayerUi() {
        val activeTrack = activityCoordinator.playbackController.currentTrack
        updateSuccessState {
            it.copy(
                currentTrack = activeTrack ?: it.currentTrack,
                isPlaying = activityCoordinator.playbackController.isPlaying
            )
        }
        val displayedTrack = (_uiState.value as? HomeUiState.Success)?.currentTrack
        syncTrackLikedStatus(displayedTrack)
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

    override fun onCleared() {
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