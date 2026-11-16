package com.vpch.vibepulse.domain.model

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