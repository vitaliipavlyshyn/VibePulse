package com.vpch.vibepulse.domain.model

data class Track(
    val spotifyUri: String,
    val title: String,
    val artist: String,
    val genre: String = "",
    val tempo: Int = 0,
    val albumArtUrl: String? = null,
    val durationMs: Long = 0L,
    val isExplicit: Boolean = false
)
