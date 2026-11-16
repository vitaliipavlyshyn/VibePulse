package com.vpch.vibepulse.domain.model

data class PlaylistPreferences(
    val weatherEnabled: Boolean = true,
    val genres: List<String> = emptyList(),
    val excludeRemixesAndCovers: Boolean = false,
    val excludeExplicit: Boolean = false
) {
    fun toggleGenre(genre: String): PlaylistPreferences {
        val exists = genres.any { it.equals(genre, ignoreCase = true) }
        return if (exists) {
            copy(genres = genres.filterNot { it.equals(genre, ignoreCase = true) })
        } else {
            copy(genres = genres + genre.trim())
        }
    }

    fun isTrackAllowed(track: Track): Boolean {
        if (excludeExplicit && track.isExplicit) {
            return false
        }

        if (excludeRemixesAndCovers) {
            val titleLower = track.title.lowercase()
            val blockedMarkers = listOf(
                "remix", "cover", "acoustic", "live", "version",
                "sped up", "slowed", "reverb", "nightcore", "karaoke",
                "instrumental", "tribute", "piano version"
            )
            if (blockedMarkers.any { titleLower.contains(it) }) {
                return false
            }
        }

        return true
    }

    companion object {
        val AVAILABLE_GENRES = listOf(
            "Pop",
            "Hip-Hop",
            "Rock",
            "Dance",
            "Electronic",
            "House",
            "Indie",
            "Drum & Bass",
            "R&B"
        )
    }
}