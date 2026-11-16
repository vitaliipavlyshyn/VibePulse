package com.vpch.vibepulse.domain.repository

import com.vpch.vibepulse.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.StateFlow

interface WeatherRepository {
    val currentWeather: StateFlow<WeatherSnapshot?>
    suspend fun getWeather(forceRefresh: Boolean = false): Result<WeatherSnapshot>
}