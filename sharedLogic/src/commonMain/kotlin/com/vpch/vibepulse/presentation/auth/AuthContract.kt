package com.vpch.vibepulse.presentation.auth



sealed interface AuthIntent {
    data object ConnectSpotifyClicked: AuthIntent
    data class AuthCodeReceived(val code: String): AuthIntent
    data class AuthErrorReceived(val error: String): AuthIntent
}

sealed interface AuthEffect {
    data class OpenBrowser(val url: String): AuthEffect
    data object NavigateToHome: AuthEffect
}
