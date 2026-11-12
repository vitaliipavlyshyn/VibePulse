package com.vpch.vibepulse.network

import com.vpch.vibepulse.PkceUtil
import com.vpch.vibepulse.generateCodeChallenge
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("scope") val scope: String,
)


class SpotifyAuthService(
    private val httpClient: HttpClient,
    private val config: SpotifyConfig
) {
    var currentCodeVerifier: String? = null
        private set

    fun buildAuthorizationUrl(): String {
        val verifier = PkceUtil.generateCodeVerifier()
        currentCodeVerifier = verifier

        val codeChallenge = generateCodeChallenge(verifier)

        val scopes = listOf(
            "user-read-private",
            "user-read-email",
            "user-modify-playback-state",
            "user-read-playback-state"
        ).joinToString("%20")

        return "https://accounts.spotify.com/authorize?" +
                "client_id=${config.clientId}" +
                "&response_type=code" +
                "&redirect_uri=${config.redirectUri}" +
                "&scope=$scopes" +
                "&code_challenge_method=S256" +
                "&code_challenge=$codeChallenge"
    }

    suspend fun exchangeCodeForToken(code: String): Result<SpotifyTokenResponse> {
        val verifier = currentCodeVerifier ?: return Result.failure(Exception("Code verifier not found"))

        return runCatching {
            httpClient.submitForm(
                url = "https://accounts.spotify.com/api/token",
                formParameters = Parameters.build {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", config.redirectUri)
                    append("client_id", config.clientId)
                    append("code_verifier", verifier)
                }
            ).body()
        }
    }
}