package com.vpch.vibepulse.domain.service

import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.location.LocationProvider
import com.vpch.vibepulse.network.WeatherApiService
import com.vpch.vibepulse.utils.toDomain
import kotlin.time.Duration

class WeatherService(
    private val weatherApiService: WeatherApiService,
    private val locationProvider: LocationProvider,
    private val cacheTtl: Duration = WeatherSnapshot.DEFAULT_CACHE_TTL
) {
    var lastSnapshot: WeatherSnapshot? = null
        private set

    suspend fun getWeather(forceRefresh: Boolean = false): Result<WeatherSnapshot> {
        val cached = lastSnapshot
        if (!forceRefresh && cached != null && cached.isFresh(cacheTtl)) {
            return Result.success(cached)
        }

        if (!locationProvider.isLocationEnabled() && cached != null) {
            return Result.success(cached)
        }

        return locationProvider.getCurrentLocation().fold(
            onSuccess = { coords ->
                weatherApiService.getCurrentWeather(coords.latitude, coords.longitude)
                    .map { it.toDomain() }
                    .onSuccess { snapshot -> lastSnapshot = snapshot }
            },
            onFailure = { error ->
                if (cached != null) Result.success(cached) else Result.failure(error)
            }
        )
    }
}