package com.vpch.vibepulse.network

import com.vpch.vibepulse.PkceUtil
import com.vpch.vibepulse.generateCodeChallenge
import com.vpch.vibepulse.storage.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("scope") val scope: String? = null,
)

@Serializable
data class SpotifyErrorResponse(
    @SerialName("error") val error: String,
    @SerialName("error_description") val errorDescription: String? = null
)

class SpotifyAuthService(
    private val httpClient: HttpClient,
    private val config: SpotifyConfig,
    private val tokenStorage: TokenStorage
) {
    var currentCodeVerifier: String? = null
        private set

    fun buildAuthorizationUrl(): String {
        val verifier = PkceUtil.generateCodeVerifier()
        currentCodeVerifier = verifier

        val codeChallenge = generateCodeChallenge(verifier)
        val authBase = config.authBaseUrl.trimEnd('/')

        val scopes = listOf(
            "user-read-private",
            "user-modify-playback-state",
            "user-read-playback-state",
            "user-library-read",
            "user-library-modify",
            "app-remote-control"
        ).joinToString("%20")

        return "$authBase/authorize?" +
                "client_id=${config.clientId}" +
                "&response_type=code" +
                "&redirect_uri=${config.redirectUri}" +
                "&scope=$scopes" +
                "&code_challenge_method=S256" +
                "&code_challenge=$codeChallenge"
    }

    suspend fun exchangeCodeForToken(code: String): Result<SpotifyTokenResponse> {
        val verifier = currentCodeVerifier
            ?: return Result.failure(IllegalStateException("Code verifier not found"))

        val tokenUrl = "${config.authBaseUrl.trimEnd('/')}/api/token"

        return runCatching {
            val response = httpClient.submitForm(
                url = tokenUrl,
                formParameters = Parameters.build {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", config.redirectUri)
                    append("client_id", config.clientId)
                    append("code_verifier", verifier)
                }
            )

            if (response.status.isSuccess()) {
                val tokens = response.body<SpotifyTokenResponse>()
                tokenStorage.saveTokens(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresInSeconds = tokens.expiresIn
                )
                currentCodeVerifier = null
                tokens
            } else {
                val errorResponse = response.body<SpotifyErrorResponse>()
                throw Exception("Spotify API Error [${errorResponse.error}]: ${errorResponse.errorDescription}")
            }
        }
    }

    suspend fun refreshAccessToken(): Result<SpotifyTokenResponse> {
        val refreshToken = tokenStorage.getRefreshToken()
            ?: return Result.failure(IllegalStateException("No refresh token available"))

        val tokenUrl = "${config.authBaseUrl.trimEnd('/')}/api/token"

        return runCatching {
            val response = httpClient.submitForm(
                url = tokenUrl,
                formParameters = Parameters.build {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                    append("client_id", config.clientId)
                }
            )

            if (response.status.isSuccess()) {
                val tokens = response.body<SpotifyTokenResponse>()
                tokenStorage.saveTokens(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken ?: refreshToken,
                    expiresInSeconds = tokens.expiresIn
                )
                tokens
            } else {
                tokenStorage.clearTokens()
                throw Exception("Session expired, please log in again")
            }
        }
    }
}