package com.vpch.vibepulse.di

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.vpch.vibepulse.location.AndroidLocationProvider
import com.vpch.vibepulse.location.LocationProvider
import com.vpch.vibepulse.player.AndroidSpotifyRemoteClient
import com.vpch.vibepulse.player.SpotifyRemoteClient
import com.vpch.vibepulse.sensor.AndroidCadenceMeter
import com.vpch.vibepulse.sensor.CadenceMeter
import com.vpch.vibepulse.service.AndroidRunTrackerNotifier
import com.vpch.vibepulse.service.RunTrackerNotifier
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<Settings> {
        val context: Context = get()
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "vibepulse_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        SharedPreferencesSettings(encryptedPrefs)
    }

    single<LocationProvider> { AndroidLocationProvider(context = get()) }
    single<CadenceMeter> { AndroidCadenceMeter(context = get()) }

    single<SpotifyRemoteClient> {
        AndroidSpotifyRemoteClient(
            context = androidContext(),
            config = get()
        )
    }

    single<RunTrackerNotifier> { AndroidRunTrackerNotifier(androidContext()) }
}

