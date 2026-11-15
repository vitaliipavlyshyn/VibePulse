package com.vpch.vibepulse.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReccoBeatsAudioFeaturesResponse(
    @SerialName("content") val content: List<ReccoBeatsAudioFeaturesDto> = emptyList()
)

@Serializable
data class ReccoBeatsAudioFeaturesDto(
    @SerialName("id") val id: String = "",
    @SerialName("href") val href: String = "",
    @SerialName("tempo") val tempo: Double? = null,
    @SerialName("energy") val energy: Double? = null,
    @SerialName("danceability") val danceability: Double? = null,
    @SerialName("valence") val valence: Double? = null,
    @SerialName("acousticness") val acousticness: Double? = null,
    @SerialName("instrumentalness") val instrumentalness: Double? = null,
    @SerialName("speechiness") val speechiness: Double? = null
) {
    fun extractSpotifyId(): String =
        href.substringAfterLast("/").substringBefore("?").trim()
}