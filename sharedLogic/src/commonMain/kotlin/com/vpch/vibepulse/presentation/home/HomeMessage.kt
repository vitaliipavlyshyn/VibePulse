package com.vpch.vibepulse.presentation.home

sealed interface HomeMessage {
    data object ProfileLoadError : HomeMessage
    data object LocationPermissionRequired : HomeMessage
    data object GpsDisabled : HomeMessage
    data object WeatherLoadError : HomeMessage
    data object ActivitySaved : HomeMessage
    data object ActivitySaveFailed : HomeMessage
    data class ActivityTooShort(val minSeconds: Long) : HomeMessage
    data object PlayerControlError : HomeMessage
    data object SkipTrackError : HomeMessage
    data object SeekTrackError : HomeMessage
    data object TrackLiked : HomeMessage
    data object TrackUnliked : HomeMessage
    data object LikeUpdateError : HomeMessage
    data class Custom(val text: String) : HomeMessage
}