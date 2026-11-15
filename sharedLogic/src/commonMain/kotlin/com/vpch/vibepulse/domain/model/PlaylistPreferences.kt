package com.vpch.vibepulse.domain.model

data class PlaylistPreferences(
    val weatherEnabled: Boolean = true,
    val genres: List<String> = emptyList(),
    val excludedArtists: List<String> = emptyList()
) {
    fun setWeatherEnabled(enabled: Boolean): PlaylistPreferences =
        copy(weatherEnabled = enabled)

    fun addGenre(genre: String): PlaylistPreferences =
        if (genre.isBlank() || genres.any { it.equals(genre, ignoreCase = true) }) this
        else copy(genres = genres + genre.trim())

    fun removeGenre(genre: String): PlaylistPreferences =
        copy(genres = genres.filterNot { it.equals(genre, ignoreCase = true) })

    fun excludeArtist(artist: String): PlaylistPreferences =
        if (artist.isBlank() || excludedArtists.any { it.equals(artist, ignoreCase = true) }) this
        else copy(excludedArtists = excludedArtists + artist.trim())

    fun allowArtist(artist: String): PlaylistPreferences =
        copy(excludedArtists = excludedArtists.filterNot { it.equals(artist, ignoreCase = true) })

    fun isTrackAllowed(track: Track): Boolean {
        if (excludedArtists.isEmpty()) return true
        return excludedArtists.none { excluded ->
            track.artist.contains(excluded, ignoreCase = true)
        }
    }
}