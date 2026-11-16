package com.vpch.vibepulse.presentation.home

import com.vpch.vibepulse.domain.model.CadenceZone
import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.WeatherSnapshot

enum class RunStatus {
    IDLE,
    RUNNING,
    PAUSED
}

data class WeatherUiState(
    val snapshot: WeatherSnapshot? = null,
    val hasPermission: Boolean = false,
    val isLocationEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class RunUiState(
    val status: RunStatus = RunStatus.IDLE,
    val currentCadence: Int = 0,
    val cadenceZone: CadenceZone = CadenceZone.IDLE,
    val elapsedSeconds: Long = 0L
)

data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isLiked: Boolean = false,
    val isLikeInProgress: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val user: User? = null,
        val weather: WeatherUiState = WeatherUiState(),
        val run: RunUiState = RunUiState(),
        val player: PlayerUiState = PlayerUiState()
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

sealed interface HomeIntent {
    data object LoadInitialData : HomeIntent
    data class LocationPermissionUpdated(val granted: Boolean) : HomeIntent
    data object RefreshWeather : HomeIntent

    data object StartRunClicked : HomeIntent
    data object PauseRunClicked : HomeIntent
    data object ResumeRunClicked : HomeIntent
    data object FinishRunClicked : HomeIntent

    data object ToggleLikeClicked : HomeIntent
    data object TogglePlayPauseClicked : HomeIntent
    data object SkipNextClicked : HomeIntent
    data class SeekTo(val positionMs: Long) : HomeIntent
    data object LogoutClicked : HomeIntent
}

sealed interface HomeEffect {
    data object RequestLocationPermission : HomeEffect
    data object NavigateToAuth : HomeEffect
    data class ShowMessage(val message: String) : HomeEffect
    data object OpenLocationSettings : HomeEffect
}