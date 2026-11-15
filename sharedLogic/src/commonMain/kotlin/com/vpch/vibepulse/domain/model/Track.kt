package com.vpch.vibepulse.domain.model

import kotlin.math.abs

data class Track(
    val spotifyUri: String,
    val title: String,
    val artist: String,
    val genre: String = "",
    val tempo: Int = 0,
    val albumArtUrl: String? = null,
    val durationMs: Long = 0L
) {
    fun matchesCadence(cadence: Int, threshold: Int = 10): Boolean {
        if (tempo <= 0 || cadence <= 0) return false
        return abs(tempo - cadence) <= threshold
    }
}
