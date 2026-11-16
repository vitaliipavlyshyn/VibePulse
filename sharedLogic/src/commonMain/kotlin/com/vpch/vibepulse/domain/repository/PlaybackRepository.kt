package com.vpch.vibepulse.domain.repository

import com.vpch.vibepulse.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

data class PlaybackSnapshot(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isRemoteConnected: Boolean = false
) {
    val remainingMs: Long
        get() = if (durationMs > 0L) (durationMs - positionMs).coerceAtLeast(0L) else Long.MAX_VALUE
}

interface PlaybackRepository {
    val state: StateFlow<PlaybackSnapshot>

    suspend fun connectRemote(): Result<Unit>
    fun disconnectRemote()

    suspend fun playBatch(tracks: List<Track>): Result<Unit>
    suspend fun resume(): Result<Unit>
    suspend fun pause(): Result<Unit>
    fun pauseImmediate()
    suspend fun skipToNext(): Result<Unit>
    suspend fun seekTo(positionMs: Long): Result<Unit>

    suspend fun syncState()
    suspend fun enqueueNextIfNeeded(nextTrackProvider: suspend () -> Track?)
    fun resetQueue()
}