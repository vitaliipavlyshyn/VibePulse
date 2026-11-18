package com.vpch.vibepulse.di

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import com.vpch.vibepulse.data.local.VibePulseDatabase
import com.vpch.vibepulse.data.local.getDatabaseBuilder
import com.vpch.vibepulse.data.local.getRoomDatabase
import com.vpch.vibepulse.location.IosLocationProvider
import com.vpch.vibepulse.location.LocationProvider
import com.vpch.vibepulse.player.IosSpotifyRemoteClient
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

    single { IosSpotifyRemoteClient(config = get()) }
    single<SpotifyRemoteClient> { get<IosSpotifyRemoteClient>() }

    single<RunTrackerNotifier> { NoOpRunTrackerNotifier() }

    single<VibePulseDatabase> { getRoomDatabase(getDatabaseBuilder()) }
    single { get<VibePulseDatabase>().activityDao() }
}