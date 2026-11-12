package com.vpch.vibepulse.network.model
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyUserProfileDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("product") val product: String? = null,
    @SerialName("images") val images: List<SpotifyImageDto> = emptyList()
)

@Serializable
data class SpotifyImageDto(
    @SerialName("url") val url: String,
    @SerialName("height") val height: Int? = null,
    @SerialName("width") val width: Int? = null
)

@Serializable
data class PlaybackStateResponse(
    @SerialName("is_playing") val isPlaying: Boolean = false,
    @SerialName("progress_ms") val progressMs: Long? = null,
    @SerialName("item") val item: TrackDto? = null,
    @SerialName("device") val device: DeviceDto? = null
)

@Serializable
data class TrackDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("uri") val uri: String,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("artists") val artists: List<ArtistDto> = emptyList(),
    @SerialName("album") val album: AlbumDto? = null
)

@Serializable
data class ArtistDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String
)

@Serializable
data class AlbumDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("images") val images: List<SpotifyImageDto> = emptyList()
)

@Serializable
data class DeviceDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @SerialName("is_active") val isActive: Boolean = false
)

@Serializable
data class PlayTrackRequest(
    @SerialName("uris") val uris: List<String>? = null,
    @SerialName("context_uri") val contextUri: String? = null,
    @SerialName("position_ms") val positionMs: Int? = null
)