package com.vpch.vibepulse.di

import com.vpch.vibepulse.BuildKonfig
import com.vpch.vibepulse.network.SpotifyApiService
import com.vpch.vibepulse.network.SpotifyAuthService
import com.vpch.vibepulse.network.SpotifyConfig
import com.vpch.vibepulse.network.createHttpClient
import com.vpch.vibepulse.network.createSpotifyApiClient
import com.vpch.vibepulse.storage.SecureTokenStorage
import com.vpch.vibepulse.storage.TokenStorage
import org.koin.dsl.module
import org.koin.core.qualifier.named

val AuthClientQualifier = named("authClient")
val ApiClientQualifier = named("apiClient")
val networkModule = module {
    single(AuthClientQualifier) { createHttpClient() }
    single(ApiClientQualifier) { createSpotifyApiClient(tokenStorage = get(), authService = get()) }

    single { SpotifyConfig(clientId = BuildKonfig.SPOTIFY_CLIENT_ID) }
    single<TokenStorage> { SecureTokenStorage(settings = get()) }
    single {
        SpotifyAuthService(
            httpClient = get(AuthClientQualifier),
            config = get(),
            tokenStorage = get()
        )
    }
    single { SpotifyApiService(httpClient = get(ApiClientQualifier)) }
}