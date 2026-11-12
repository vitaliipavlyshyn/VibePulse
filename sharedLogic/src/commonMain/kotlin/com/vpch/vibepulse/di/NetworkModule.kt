package com.vpch.vibepulse.di

import com.vpch.vibepulse.BuildKonfig
import com.vpch.vibepulse.network.SpotifyAuthService
import com.vpch.vibepulse.network.SpotifyConfig
import com.vpch.vibepulse.network.createHttpClient
import org.koin.dsl.module


val networkModule = module {
    single { createHttpClient() }

    single {
        SpotifyConfig(
            clientId = BuildKonfig.SPOTIFY_CLIENT_ID
        )
    }

    single { SpotifyAuthService(get(), get()) }
}