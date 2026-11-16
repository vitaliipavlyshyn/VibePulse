package com.vpch.vibepulse.presentation.home

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