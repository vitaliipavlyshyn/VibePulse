package com.vpch.vibepulse.player

import kotlinx.coroutines.flow.StateFlow

data class RemotePlaybackState(
    val trackUri: String,
    val title: String,
    val artist: String,
    val isPaused: Boolean,
    val positionMs: Long,
    val durationMs: Long
)

interface SpotifyRemoteClient {
    val isConnected: Boolean
    val playbackState: StateFlow<RemotePlaybackState?>

    suspend fun connect(): Result<Unit>
    fun disconnect()

    suspend fun play(uri: String): Result<Unit>
    suspend fun queue(uri: String): Result<Unit>
    suspend fun pause(): Result<Unit>
    fun pauseImmediate()
    suspend fun resume(): Result<Unit>
    suspend fun skipNext(): Result<Unit>
    suspend fun seekTo(positionMs: Long): Result<Unit>
}