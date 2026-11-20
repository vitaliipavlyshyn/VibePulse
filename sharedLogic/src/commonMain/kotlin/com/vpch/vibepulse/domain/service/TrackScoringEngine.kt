package com.vpch.vibepulse.domain.service

import com.vpch.vibepulse.domain.model.PaceZone
import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.TrackAudioFeatures
import com.vpch.vibepulse.domain.model.TrackCandidate
import com.vpch.vibepulse.domain.model.WeatherSnapshot
import kotlin.math.abs
import kotlin.math.min

class TrackScoringEngine {

    private val spamKeywords = listOf(
        "ringtone", "white noise", "lullaby", "8d audio"
    )

    private val russianCyrillicRegex = Regex("[ыэъёЫЭЪЁ]")
    private val russianMarkers = listOf(
        "русс", "российск", "рф", "москв", "питер", "шансон"
    )

    fun isRussianTrack(candidate: TrackCandidate): Boolean {
        val title = candidate.track.title
        val artist = candidate.track.artist
        if (russianCyrillicRegex.containsMatchIn(title) || russianCyrillicRegex.containsMatchIn(artist)) {
            return true
        }
        val combinedLower = "${title.lowercase()} ${artist.lowercase()}"
        return russianMarkers.any { combinedLower.contains(it) }
    }
    
    fun selectGenres(
        zone: PaceZone,
        weather: WeatherSnapshot?,
        preferences: PlaylistPreferences?
    ): List<String> {
        val userGenres = preferences?.genres?.filter { it.isNotBlank() }.orEmpty()
        if (userGenres.isNotEmpty()) {
            return userGenres.map(::normalizeGenreForSpotify).shuffled().take(2)
        }

        val condition = if (preferences?.weatherEnabled != false) {
            weather?.conditionMain?.lowercase().orEmpty()
        } else {
            ""
        }

        val genrePool = when (zone) {
            PaceZone.WALK -> when (condition) {
                "rain", "drizzle", "thunderstorm" -> listOf("indie", "r-n-b", "soul", "alternative")
                "clear" -> listOf("pop", "funk", "indie pop", "r-n-b")
                else -> listOf("pop", "indie", "r-n-b", "alternative")
            }
            PaceZone.JOG -> when (condition) {
                "rain", "drizzle", "thunderstorm" -> listOf("hip-hop", "indie rock", "alternative", "electronic")
                "clear" -> listOf("pop", "dance", "house", "hip-hop")
                else -> listOf("pop", "hip-hop", "indie rock", "dance")
            }
            PaceZone.RUN -> when (condition) {
                "rain", "drizzle", "thunderstorm" -> listOf("hip-hop", "rock", "electronic", "rap")
                "clear" -> listOf("dance", "pop", "hip-hop", "house")
                else -> listOf("hip-hop", "dance", "rock", "pop")
            }
            PaceZone.SPRINT -> listOf("rock", "hip-hop", "electronic", "drum-and-bass", "rap")
        }

        return genrePool.shuffled().take(2)
    }

    fun isSpam(candidate: TrackCandidate): Boolean {
        val titleLower = candidate.track.title.lowercase()
        val artistLower = candidate.track.artist.lowercase()
        return spamKeywords.any { keyword ->
            titleLower.contains(keyword) || artistLower.contains(keyword)
        }
    }

    fun score(
        features: TrackAudioFeatures?,
        popularity: Int,
        targetCadence: Int,
        zone: PaceZone,
        weather: WeatherSnapshot?,
        weatherEnabled: Boolean
    ): Double {
        val popularityScore = (popularity / 100.0).coerceIn(0.0, 1.0)
        if (features == null) {
            return (0.35 * 0.65) + (popularityScore * 0.35)
        }

        val bpmScore = calculateBpmScore(features.tempo, targetCadence)
        val vibeScore = calculateVibeScore(features, zone, weather, weatherEnabled)

        return (bpmScore * 0.50) + (vibeScore * 0.25) + (popularityScore * 0.25)
    }

    private fun calculateBpmScore(trackBpm: Double?, targetCadence: Int): Double {
        if (trackBpm == null || trackBpm <= 0.0 || targetCadence <= 0) return 0.5

        val target = targetCadence.toDouble()
        val directDiff = abs(trackBpm - target)
        val halfTimeDiff = abs((trackBpm * 2.0) - target) + 4.0
        val doubleTimeDiff = abs((trackBpm / 2.0) - target) + 6.0

        val effectiveDiff = min(directDiff, min(halfTimeDiff, doubleTimeDiff))
        return (1.0 - (effectiveDiff / 40.0)).coerceIn(0.0, 1.0)
    }

    private fun calculateVibeScore(
        features: TrackAudioFeatures,
        zone: PaceZone,
        weather: WeatherSnapshot?,
        weatherEnabled: Boolean
    ): Double {
        val targetEnergy = when (zone) {
            PaceZone.WALK -> 0.50
            PaceZone.JOG -> 0.70
            PaceZone.RUN -> 0.85
            PaceZone.SPRINT -> 0.95
        }
        val targetDanceability = when (zone) {
            PaceZone.WALK -> 0.65
            PaceZone.JOG -> 0.72
            PaceZone.RUN -> 0.68
            PaceZone.SPRINT -> 0.60
        }
        val targetValence = if (weatherEnabled && weather != null) {
            when (weather.conditionMain.lowercase()) {
                "clear" -> 0.75
                "clouds" -> 0.50
                "rain", "drizzle", "thunderstorm" -> 0.35
                "snow" -> 0.60
                else -> 0.55
            }
        } else {
            0.60
        }

        val energyScore = features.energy?.let { 1.0 - abs(it - targetEnergy) } ?: 0.5
        val danceScore = features.danceability?.let { 1.0 - abs(it - targetDanceability) } ?: 0.5
        val valenceScore = features.valence?.let { 1.0 - abs(it - targetValence) } ?: 0.5

        return ((energyScore * 0.50) + (danceScore * 0.25) + (valenceScore * 0.25))
            .coerceIn(0.0, 1.0)
    }

    private fun normalizeGenreForSpotify(raw: String): String = when (raw.trim().lowercase()) {
        "drum & bass", "drum and bass" -> "drum-and-bass"
        "r&b", "rnb" -> "r-n-b"
        "hip-hop", "hip hop" -> "hip-hop"
        else -> raw.trim().lowercase()
    }
}