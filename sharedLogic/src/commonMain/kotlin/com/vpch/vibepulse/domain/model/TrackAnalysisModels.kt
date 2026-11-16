package com.vpch.vibepulse.domain.model

data class TrackCandidate(
    val track: Track,
    val popularity: Int,
    val isPlayable: Boolean
)

data class TrackAudioFeatures(
    val tempo: Double? = null,
    val energy: Double? = null,
    val danceability: Double? = null,
    val valence: Double? = null,
    val instrumentalness: Double? = null
)