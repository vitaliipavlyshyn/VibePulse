package com.vpch.vibepulse.data.repository.remote

import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.domain.repository.WeatherRepository
import com.vpch.vibepulse.location.LocationProvider
import com.vpch.vibepulse.network.WeatherApiService
import com.vpch.vibepulse.utils.toDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration

class WeatherRepositoryImpl(
    private val weatherApiService: WeatherApiService,
    private val locationProvider: LocationProvider,
    private val cacheTtl: Duration = WeatherSnapshot.DEFAULT_CACHE_TTL
) : WeatherRepository {

    private val _currentWeather = MutableStateFlow<WeatherSnapshot?>(null)
    override val currentWeather: StateFlow<WeatherSnapshot?> = _currentWeather.asStateFlow()

    override suspend fun getWeather(forceRefresh: Boolean): Result<WeatherSnapshot> {
        val cached = _currentWeather.value
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
                    .onSuccess { snapshot -> _currentWeather.value = snapshot }
            },
            onFailure = { error ->
                if (cached != null) Result.success(cached) else Result.failure(error)
            }
        )
    }
}