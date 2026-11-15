package com.vpch.vibepulse.network


import com.vpch.vibepulse.network.model.ReccoBeatsAudioFeaturesDto
import com.vpch.vibepulse.network.model.ReccoBeatsAudioFeaturesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

class ReccoBeatsApiService(
    private val httpClient: HttpClient
) {
    private companion object {
        const val AUDIO_FEATURES_ENDPOINT = "audio-features"
        const val MAX_BATCH_SIZE = 40
    }
    suspend fun getAudioFeatures(
        spotifyTrackIds: List<String>
    ): Result<Map<String, ReccoBeatsAudioFeaturesDto>> = runCatching {
        val cleanIds = spotifyTrackIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (cleanIds.isEmpty()) return@runCatching emptyMap()

        val resultMap = mutableMapOf<String, ReccoBeatsAudioFeaturesDto>()

        cleanIds.chunked(MAX_BATCH_SIZE).forEach { batch ->
            val response = httpClient.get(AUDIO_FEATURES_ENDPOINT) {
                parameter("ids", batch.joinToString(","))
            }

            if (response.status.isSuccess()) {
                val payload = response.body<ReccoBeatsAudioFeaturesResponse>()
                payload.content.forEach { dto ->
                    val spotifyId = dto.extractSpotifyId()
                    if (spotifyId.isNotBlank()) {
                        resultMap[spotifyId] = dto
                    }
                }
            }
        }
        resultMap
    }
}