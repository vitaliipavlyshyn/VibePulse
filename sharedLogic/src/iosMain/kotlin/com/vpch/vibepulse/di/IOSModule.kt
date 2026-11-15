package com.vpch.vibepulse.di

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import com.vpch.vibepulse.location.IosLocationProvider
import com.vpch.vibepulse.location.LocationProvider
import com.vpch.vibepulse.player.RemotePlaybackState
import com.vpch.vibepulse.player.SpotifyRemoteClient
import com.vpch.vibepulse.sensor.CadenceMeter
import com.vpch.vibepulse.sensor.IosCadenceMeter
import com.vpch.vibepulse.service.NoOpRunTrackerNotifier
import com.vpch.vibepulse.service.RunTrackerNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.dsl.module

@OptIn(ExperimentalSettingsImplementation::class)
val iosModule = module {
    single<Settings> {
        KeychainSettings(service = "com.vpch.vibepulse")
    }

    single<LocationProvider> { IosLocationProvider() }
    single<CadenceMeter> { IosCadenceMeter() }

    single<SpotifyRemoteClient> {
        object : SpotifyRemoteClient {
            override val isConnected: Boolean = false
            override val playbackState: StateFlow<RemotePlaybackState?> = MutableStateFlow(null)
            override suspend fun connect(): Result<Unit> = Result.failure(UnsupportedOperationException("iOS SDK not linked yet"))
            override fun disconnect() {}
            override suspend fun play(uri: String): Result<Unit> = Result.failure(UnsupportedOperationException())
            override suspend fun queue(uri: String): Result<Unit> = Result.failure(UnsupportedOperationException())
            override suspend fun pause(): Result<Unit> = Result.failure(UnsupportedOperationException())
            override fun pauseImmediate() {
                TODO("Not yet implemented")
            }

            override suspend fun resume(): Result<Unit> = Result.failure(UnsupportedOperationException())
            override suspend fun skipNext(): Result<Unit> = Result.failure(UnsupportedOperationException())
        }
    }

    single<RunTrackerNotifier> { NoOpRunTrackerNotifier() }
}