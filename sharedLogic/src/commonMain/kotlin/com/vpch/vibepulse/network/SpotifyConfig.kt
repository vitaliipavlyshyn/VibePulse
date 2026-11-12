package com.vpch.vibepulse.network

data class SpotifyConfig(
    val clientId: String,
    val redirectUri: String = "vibepulse://spotify-callback"
)
