package com.vpch.vibepulse.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    @SerialName("name") val cityName: String = "",
    @SerialName("weather") val weather: List<WeatherConditionDto> = emptyList(),
    @SerialName("main") val main: MainWeatherDto,
    @SerialName("wind") val wind: WindDto? = null
)

@Serializable
data class WeatherConditionDto(
    @SerialName("id") val id: Int,
    @SerialName("main") val main: String,
    @SerialName("description") val description: String,
    @SerialName("icon") val icon: String
)

@Serializable
data class MainWeatherDto(
    @SerialName("temp") val temp: Double,
    @SerialName("feels_like") val feelsLike: Double,
    @SerialName("humidity") val humidity: Int
)

@Serializable
data class WindDto(
    @SerialName("speed") val speed: Double
)