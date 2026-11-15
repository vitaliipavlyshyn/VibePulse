package com.vpch.vibepulse.di

import com.vpch.vibepulse.domain.coordinator.ActivityCoordinator
import com.vpch.vibepulse.domain.service.PlaybackController
import com.vpch.vibepulse.domain.service.PlaylistGenerator
import com.vpch.vibepulse.domain.service.WeatherService
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    single { WeatherService(weatherApiService = get(), locationProvider = get()) }
    singleOf(::PlaybackController)
    factory { PlaylistGenerator(spotifyApiService = get(), reccoBeatsApiService = get()) }
    factoryOf(::ActivityCoordinator)
}