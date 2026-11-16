package com.vpch.vibepulse.di

import com.vpch.vibepulse.BuildKonfig
import com.vpch.vibepulse.network.ReccoBeatsApiService
import com.vpch.vibepulse.network.SpotifyApiService
import com.vpch.vibepulse.network.SpotifyAuthService
import com.vpch.vibepulse.network.SpotifyConfig
import com.vpch.vibepulse.network.WeatherApiService
import com.vpch.vibepulse.network.WeatherConfig
import com.vpch.vibepulse.network.createHttpClient
import com.vpch.vibepulse.network.createReccoBeatsApiClient
import com.vpch.vibepulse.network.createSpotifyApiClient
import com.vpch.vibepulse.storage.SecureTokenStorage
import com.vpch.vibepulse.storage.TokenStorage
import org.koin.dsl.module
import org.koin.core.qualifier.named
val SpotifyApiClientQualifier = named("spotifyApiClient")
val ReccoBeatsApiClientQualifier = named("reccoBeatsApiClient")

val networkModule = module {
    single { createHttpClient() }

    single(SpotifyApiClientQualifier) {
        createSpotifyApiClient(
            baseUrl = BuildKonfig.SPOTIFY_BASE_URL,
            tokenStorage = get(),
            authService = get()
        )
    }

    single(ReccoBeatsApiClientQualifier) {
        createReccoBeatsApiClient(
            baseUrl = BuildKonfig.RECCOBEATS_BASE_URL
        )
    }

    single {
        SpotifyConfig(
            clientId = BuildKonfig.SPOTIFY_CLIENT_ID,
            redirectUri = BuildKonfig.SPOTIFY_REDIRECT_URI,
            authBaseUrl = BuildKonfig.SPOTIFY_AUTH_BASE_URL
        )
    }
    single {
        SpotifyAuthService(
            httpClient = get(),
            config = get(),
            tokenStorage = get()
        )
    }
    single {
        SpotifyApiService(
            httpClient = get(SpotifyApiClientQualifier)
        )
    }

    single {
        WeatherConfig(
            apiKey = BuildKonfig.OPENWEATHER_API_KEY,
            weatherBaseUrl = BuildKonfig.OPENWEATHER_BASE_URL,
            geoBaseUrl = BuildKonfig.OPENWEATHER_GEO_BASE_URL
        )
    }
    single {
        WeatherApiService(
            httpClient = get(),
            config = get()
        )
    }
    single {
        ReccoBeatsApiService(
            httpClient = get(ReccoBeatsApiClientQualifier)
        )
    }
}