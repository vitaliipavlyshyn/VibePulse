package com.vpch.vibepulse.network

import com.vpch.vibepulse.BuildKonfig

data class WeatherConfig(
    val apiKey: String,
    val weatherBaseUrl: String,
    val geoBaseUrl: String
)
