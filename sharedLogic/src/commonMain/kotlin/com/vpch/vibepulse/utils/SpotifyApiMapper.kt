package com.vpch.vibepulse.utils

import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.network.model.SpotifyUserProfileDto
import com.vpch.vibepulse.network.model.TrackDto
import com.vpch.vibepulse.network.model.WeatherResponse

fun SpotifyUserProfileDto.toDomain(): User = User(
    spotifyId = id,
    displayName = displayName ?: "Runner",
    hasPremium = product.equals("premium", ignoreCase = true),
    avatarUrl = images.firstOrNull()?.url
)

fun TrackDto.toDomain(): Track {
    val validUri = uri.takeIf { it.startsWith("spotify:track:") }
        ?: "spotify:track:$id"

    return Track(
        spotifyUri = validUri,
        title = name,
        artist = artists.joinToString(", ") { it.name },
        albumArtUrl = album?.images?.firstOrNull()?.url,
        durationMs = durationMs
    )
}

fun WeatherResponse.toDomain(): WeatherSnapshot = WeatherSnapshot(
    area = cityName,
    temperature = main.temp,
    condition = weather.firstOrNull()?.main ?: "Clear"
)