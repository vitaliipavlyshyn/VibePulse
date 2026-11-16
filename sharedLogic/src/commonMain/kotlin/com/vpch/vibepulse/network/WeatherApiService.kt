package com.vpch.vibepulse.network

import com.vpch.vibepulse.network.model.GeoLocationDto
import com.vpch.vibepulse.network.model.WeatherResponse
import com.vpch.vibepulse.utils.getSystemLanguageCode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


class WeatherApiService(
    private val httpClient: HttpClient,
    private val config: WeatherConfig
) {
    suspend fun getCurrentWeather(
        lat: Double,
        lon: Double,
        lang: String = resolveApiLanguage()
    ): Result<WeatherResponse> = runCatching {
        coroutineScope {
            val weatherUrl = "${config.weatherBaseUrl.trimEnd('/')}/weather"
            val geoUrl = "${config.geoBaseUrl.trimEnd('/')}/reverse"

            val weatherDeferred = async {
                httpClient.get(weatherUrl) {
                    parameter("lat", lat)
                    parameter("lon", lon)
                    parameter("appid", config.apiKey)
                    parameter("units", "metric")
                    parameter("lang", lang)
                }
            }

            val geoDeferred = async {
                runCatching {
                    httpClient.get(geoUrl) {
                        parameter("lat", lat)
                        parameter("lon", lon)
                        parameter("limit", 1)
                        parameter("appid", config.apiKey)
                    }
                }.getOrNull()
            }

            val weatherResponse = weatherDeferred.await()
            if (!weatherResponse.status.isSuccess()) {
                throw Exception("Weather API error: HTTP ${weatherResponse.status.value}")
            }

            val weather = weatherResponse.body<WeatherResponse>()
            val geoList = geoDeferred.await()
                ?.takeIf { it.status.isSuccess() }
                ?.body<List<GeoLocationDto>>()
                .orEmpty()

            val localizedCity = geoList.firstOrNull()?.let { geo ->
                geo.localNames[lang] ?: geo.name
            }?.takeIf { it.isNotBlank() }


            if (!weatherResponse.status.isSuccess()) {
                val errorBody = runCatching { weatherResponse.body<String>() }.getOrDefault("")
                throw Exception("Weather API (${weatherResponse.status.value}): $errorBody")
            }

            if (localizedCity != null) {
                weather.copy(cityName = localizedCity)
            } else {
                weather
            }

        }
    }

    private fun resolveApiLanguage(): String =
        when (getSystemLanguageCode().lowercase()) {
            "uk" -> "uk"
            else -> "en"
        }
}