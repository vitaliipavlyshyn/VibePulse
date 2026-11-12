package com.vpch.vibepulse.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("spotify_id") val spotifyId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("has_premium") val hasPremium: Boolean
)
