package com.vpch.vibepulse.network


import com.vpch.vibepulse.network.model.PlayTrackRequest
import com.vpch.vibepulse.network.model.PlaybackStateResponse
import com.vpch.vibepulse.network.model.SpotifyUserProfileDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class SpotifyApiService(
    private val httpClient: HttpClient
) {
    suspend fun getCurrentUser(): Result<SpotifyUserProfileDto> = runCatching {
        val response = httpClient.get("me")
        if (response.status.isSuccess()) {
            response.body<SpotifyUserProfileDto>()
        } else {
            throw Exception("Failed to load profile: HTTP ${response.status.value}")
        }
    }

    suspend fun getPlaybackState(): Result<PlaybackStateResponse?> = runCatching {
        val response = httpClient.get("me/player")
        when {
            response.status == HttpStatusCode.NoContent -> null
            response.status.isSuccess() -> response.body<PlaybackStateResponse>()
            else -> throw Exception("Failed to get playback state: HTTP ${response.status.value}")
        }
    }

    suspend fun play(
        uris: List<String>? = null,
        contextUri: String? = null
    ): Result<Unit> = executePlaybackCommand {
        httpClient.put("me/player/play") {
            if (uris != null || contextUri != null) {
                contentType(ContentType.Application.Json)
                setBody(PlayTrackRequest(uris = uris, contextUri = contextUri))
            }
        }
    }

    suspend fun pause(): Result<Unit> = executePlaybackCommand {
        httpClient.put("me/player/pause")
    }

    suspend fun skipToNext(): Result<Unit> = executePlaybackCommand {
        httpClient.post("me/player/next")
    }

    private suspend inline fun executePlaybackCommand(
        crossinline request: suspend () -> HttpResponse
    ): Result<Unit> = runCatching {
        val response = request()
        when {
            response.status.isSuccess() -> Unit
            response.status == HttpStatusCode.NotFound ->
                throw IllegalStateException("No active Spotify device found. Open Spotify on your device first.")
            response.status == HttpStatusCode.Forbidden ->
                throw IllegalStateException("Spotify Premium is required for playback control.")
            else ->
                throw Exception("Playback error: HTTP ${response.status.value}")
        }
    }
}