package com.vpch.vibepulse.utils

import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.network.model.WeatherResponse

fun WeatherResponse.toDomain(): WeatherSnapshot {
    val firstWeather = weather.firstOrNull()
    val descriptionText = firstWeather?.description
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        ?: firstWeather?.main
        ?: ""

    val iconUrl = firstWeather?.icon
        ?.takeIf { it.isNotBlank() }
        ?.let { code -> "https://openweathermap.org/img/wn/$code@2x.png" }

    return WeatherSnapshot(
        area = cityName,
        temperature = main.temp,
        condition = descriptionText,
        conditionMain = firstWeather?.main.orEmpty(),
        iconUrl = iconUrl
    )
}