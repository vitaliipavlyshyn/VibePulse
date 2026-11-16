package com.vpch.vibepulse.network

import com.vpch.vibepulse.BuildKonfig

data class SpotifyConfig(
    val clientId: String,
    val redirectUri: String,
    val authBaseUrl: String
)