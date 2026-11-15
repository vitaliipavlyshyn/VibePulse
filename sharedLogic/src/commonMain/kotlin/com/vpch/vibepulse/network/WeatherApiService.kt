package com.vpch.vibepulse.network

import com.vpch.vibepulse.network.model.WeatherResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

data class WeatherConfig(
    val apiKey: String
)

class WeatherApiService(
    private val httpClient: HttpClient,
    private val config: WeatherConfig
) {
    suspend fun getCurrentWeather(
        lat: Double,
        lon: Double
    ): Result<WeatherResponse> = runCatching {
        val response = httpClient.get("https://api.openweathermap.org/data/2.5/weather") {
            parameter("lat", lat)
            parameter("lon", lon)
            parameter("appid", config.apiKey)
            parameter("units", "metric")
        }

        if (response.status.isSuccess()) {
            response.body<WeatherResponse>()
        } else {
            throw Exception("Weather API error: HTTP ${response.status.value}")
        }
    }
}