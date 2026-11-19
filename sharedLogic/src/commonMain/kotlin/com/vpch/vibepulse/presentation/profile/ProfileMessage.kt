package com.vpch.vibepulse.presentation.profile

sealed interface ProfileMessage {
    data object ProfileLoadError : ProfileMessage
    data object LogoutError : ProfileMessage
    data class Custom(val text: String) : ProfileMessage
}