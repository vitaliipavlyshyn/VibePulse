package com.vpch.vibepulse.data.playback

import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.network.SpotifyApiService
import com.vpch.vibepulse.player.SpotifyRemoteClient
import com.vpch.vibepulse.utils.toDomain
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

data class ExternalPlaybackState(
    val trackUri: String?,
    val resolvedTrack: Track?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long
)

interface PlaybackGateway {
    val isAvailable: Boolean
    suspend fun playBatch(tracks: List<Track>): Result<Unit>
    suspend fun enqueue(trackUri: String): Result<Unit>
    suspend fun resume(fallbackTracks: List<Track>): Result<Unit>
    suspend fun pause(): Result<Unit>
    suspend fun skipNext(fallbackTrack: Track?): Result<Unit>
    suspend fun seekTo(positionMs: Long): Result<Unit>
    suspend fun fetchState(): ExternalPlaybackState?
}

