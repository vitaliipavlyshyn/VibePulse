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
    val item: TrackDto? = null,
    val actions: PlaybackActionsDto? = null
)

@Serializable
data class PlaybackActionsDto(
    val disallows: PlaybackDisallowsDto? = null
)

@Serializable
data class PlaybackDisallowsDto(
    val pausing: Boolean = false,
    val resuming: Boolean = false,
    @SerialName("skipping_next") val skippingNext: Boolean = false
)

@Serializable
data class TrackDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("uri") val uri: String,
    @SerialName("popularity") val popularity: Int = 0,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("artists") val artists: List<ArtistDto> = emptyList(),
    @SerialName("album") val album: AlbumDto? = null,
    @SerialName("is_playable") val isPlayable: Boolean = true,
    @SerialName("explicit") val explicit: Boolean = false
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
    @SerialName("name") val name: String = "",
    @SerialName("type") val type: String = "",
    @SerialName("is_active") val isActive: Boolean = false
)

@Serializable
data class SpotifySearchResponse(
    @SerialName("tracks") val tracks: TracksPageDto? = null
)

@Serializable
data class TracksPageDto(
    @SerialName("items") val items: List<TrackDto> = emptyList()
)

@Serializable
data class DevicesResponse(
    @SerialName("devices") val devices: List<DeviceDto> = emptyList()
)

@Serializable
data class PlayTrackRequest(
    @SerialName("uris") val uris: List<String>
)