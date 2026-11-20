package com.vpch.vibepulse.domain.repository

import com.vpch.vibepulse.domain.model.PlaybackSnapshot
import com.vpch.vibepulse.domain.model.Track
import kotlinx.coroutines.flow.StateFlow


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
    fun clearPlaybackState()
}