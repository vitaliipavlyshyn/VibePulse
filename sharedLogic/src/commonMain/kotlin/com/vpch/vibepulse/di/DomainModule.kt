package com.vpch.vibepulse.di

import com.vpch.vibepulse.data.playback.PlaybackRepositoryImpl
import com.vpch.vibepulse.data.playback.RemotePlaybackGateway
import com.vpch.vibepulse.data.playback.WebApiPlaybackGateway
import com.vpch.vibepulse.data.repository.AuthRepositoryImpl
import com.vpch.vibepulse.data.repository.TrackRepositoryImpl
import com.vpch.vibepulse.data.repository.UserRepositoryImpl
import com.vpch.vibepulse.data.repository.WeatherRepositoryImpl
import com.vpch.vibepulse.domain.coordinator.ActivityCoordinator
import com.vpch.vibepulse.domain.repository.AuthRepository
import com.vpch.vibepulse.domain.repository.PlaybackRepository
import com.vpch.vibepulse.domain.repository.TrackRepository
import com.vpch.vibepulse.domain.repository.UserRepository
import com.vpch.vibepulse.domain.repository.WeatherRepository
import com.vpch.vibepulse.domain.service.TrackScoringEngine
import com.vpch.vibepulse.domain.usecase.GenerateWorkoutPlaylistUseCase
import com.vpch.vibepulse.domain.usecase.LogoutUseCase
import com.vpch.vibepulse.domain.usecase.ToggleTrackLikeUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val domainModule = module {
    singleOf(::RemotePlaybackGateway)
    singleOf(::WebApiPlaybackGateway)
    singleOf(::PlaybackRepositoryImpl) bind PlaybackRepository::class
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::UserRepositoryImpl) bind UserRepository::class
    single<WeatherRepository> {
        WeatherRepositoryImpl(
            weatherApiService = get(),
            locationProvider = get()
        )
    }
    singleOf(::TrackRepositoryImpl) bind TrackRepository::class

    factoryOf(::TrackScoringEngine)
    factoryOf(::GenerateWorkoutPlaylistUseCase)
    factoryOf(::ToggleTrackLikeUseCase)
    factoryOf(::LogoutUseCase)

    singleOf(::ActivityCoordinator)
}