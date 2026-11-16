package com.vpch.vibepulse.network

import com.vpch.vibepulse.network.model.DevicesResponse
import com.vpch.vibepulse.network.model.PlayTrackRequest
import com.vpch.vibepulse.network.model.PlaybackStateResponse
import com.vpch.vibepulse.network.model.SpotifySearchResponse
import com.vpch.vibepulse.network.model.SpotifyUserProfileDto
import com.vpch.vibepulse.network.model.TrackDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
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

    suspend fun play(uris: List<String>? = null): Result<Unit> = executePlaybackCommand {
        val targetDeviceId = resolveTargetDeviceId()
        httpClient.put("me/player/play") {
            if (targetDeviceId != null) {
                parameter("device_id", targetDeviceId)
            }
            if (uris != null) {
                contentType(ContentType.Application.Json)
                setBody(PlayTrackRequest(uris = uris))
            }
        }
    }

    suspend fun pause(): Result<Unit> = executePlaybackCommand {
        val targetDeviceId = resolveTargetDeviceId()
        httpClient.put("me/player/pause") {
            if (targetDeviceId != null) {
                parameter("device_id", targetDeviceId)
            }
        }
    }

    suspend fun skipToNext(): Result<Unit> = executePlaybackCommand {
        val targetDeviceId = resolveTargetDeviceId()
        httpClient.post("me/player/next") {
            if (targetDeviceId != null) {
                parameter("device_id", targetDeviceId)
            }
        }
    }

    suspend fun searchTracks(
        query: String,
        limit: Int = 25,
        offset: Int = 0
    ): Result<List<TrackDto>> = runCatching {
        val response = httpClient.get("search") {
            parameter("q", query)
            parameter("type", "track")
            parameter("market", "from_token")
            parameter("limit", limit)
            parameter("offset", offset)
        }
        if (response.status.isSuccess()) {
            response.body<SpotifySearchResponse>().tracks?.items.orEmpty()
        } else {
            throw Exception("Spotify search error: HTTP ${response.status.value}")
        }
    }
    suspend fun addToQueue(trackUri: String): Result<Unit> = executePlaybackCommand {
        val targetDeviceId = resolveTargetDeviceId()
        httpClient.post("me/player/queue") {
            parameter("uri", trackUri)
            if (targetDeviceId != null) {
                parameter("device_id", targetDeviceId)
            }
        }
    }

    private suspend fun resolveTargetDeviceId(): String? {
        val devices = runCatching {
            val res = httpClient.get("me/player/devices")
            if (res.status.isSuccess()) res.body<DevicesResponse>().devices else emptyList()
        }.getOrDefault(emptyList())

        val activeDevice = devices.firstOrNull { it.isActive }
        if (activeDevice != null) return null

        return (devices.firstOrNull { it.type.equals("Smartphone", ignoreCase = true) }
            ?: devices.firstOrNull())?.id
    }

    private suspend inline fun executePlaybackCommand(
        crossinline block: suspend () -> HttpResponse
    ): Result<Unit> = runCatching {
        val response = block()
        if (response.status.isSuccess()) {
            return@runCatching
        }
        val errorBody = response.bodyAsText()
        val message = when {
            response.status.value == 401 -> "Сесія Spotify застаріла. Увійдіть повторно."
            response.status.value == 403 && errorBody.contains("PREMIUM_REQUIRED", ignoreCase = true) ->
                "Для керування відтворенням потрібна підписка Spotify Premium."
            response.status.value == 403 ->
                "Дія недоступна в поточному стані плеєра (403)."
            response.status.value == 404 ->
                "Не знайдено активний пристрій Spotify. Відкрийте додаток Spotify."
            else -> "Помилка плеєра (${response.status.value}): $errorBody"
        }
        throw Exception(message)
    }


    suspend fun isTrackSaved(trackId: String): Result<Boolean> = runCatching {
        val cleanId = trackId.substringAfterLast(":").trim()
        if (cleanId.isBlank()) return@runCatching false

        val response = httpClient.get("me/tracks/contains") {
            parameter("ids", cleanId)
        }
        if (response.status.isSuccess()) {
            response.body<List<Boolean>>().firstOrNull() ?: false
        } else {
            throw Exception("Failed to check saved track: HTTP ${response.status.value}")
        }
    }

    suspend fun seekTo(positionMs: Long): Result<Unit> = executePlaybackCommand {
        val targetDeviceId = resolveTargetDeviceId()
        httpClient.put("me/player/seek") {
            parameter("position_ms", positionMs.coerceAtLeast(0L))
            if (targetDeviceId != null) {
                parameter("device_id", targetDeviceId)
            }
        }
    }

    suspend fun saveTrack(trackId: String): Result<Unit> = executeLibraryCommand {
        val cleanId = trackId.substringAfterLast(":").trim()
        httpClient.put("me/tracks") {
            parameter("ids", cleanId)
        }
    }

    suspend fun removeSavedTrack(trackId: String): Result<Unit> = executeLibraryCommand {
        val cleanId = trackId.substringAfterLast(":").trim()
        httpClient.delete("me/tracks") {
            parameter("ids", cleanId)
        }
    }

    private suspend inline fun executeLibraryCommand(
        crossinline block: suspend () -> HttpResponse
    ): Result<Unit> = runCatching {
        val response = block()
        if (response.status.isSuccess()) return@runCatching

        val message = when (response.status.value) {
            401 -> "Сесія Spotify застаріла. Увійдіть повторно."
            403 -> "Немає дозволу на зміну медіатеки. Перезайдіть в акаунт Spotify."
            else -> "Помилка оновлення улюблених (${response.status.value})"
        }
        throw Exception(message)
    }
}