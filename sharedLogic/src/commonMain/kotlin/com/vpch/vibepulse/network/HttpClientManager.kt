package com.vpch.vibepulse.network

import com.vpch.vibepulse.storage.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


private fun HttpClientConfig<*>.installBasePlugins() {
    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            isLenient = true
            coerceInputValues = true
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.ALL
        sanitizeHeader { header -> header == io.ktor.http.HttpHeaders.Authorization }
    }
}

fun createHttpClient() = HttpClient {
    installBasePlugins()
}

fun createSpotifyApiClient(
    baseUrl: String,
    tokenStorage: TokenStorage,
    authService: SpotifyAuthService
) = HttpClient {
    installBasePlugins()

    defaultRequest {
        url(baseUrl)
    }

    install(Auth) {
        bearer {
            loadTokens {
                val access = tokenStorage.getAccessToken()
                val refresh = tokenStorage.getRefreshToken()
                if (access != null && refresh != null) {
                    BearerTokens(accessToken = access, refreshToken = refresh)
                } else {
                    null
                }
            }

            refreshTokens {
                authService.refreshAccessToken().getOrNull()?.let { newTokens ->
                    BearerTokens(
                        accessToken = newTokens.accessToken,
                        refreshToken = newTokens.refreshToken ?: oldTokens?.refreshToken.orEmpty()
                    )
                }
            }

            sendWithoutRequest { request ->
                request.url.host == "api.spotify.com"
            }
        }
    }
}

fun createReccoBeatsApiClient(
    baseUrl: String
) = HttpClient {
    installBasePlugins()

    defaultRequest {
        url(baseUrl)
    }
}