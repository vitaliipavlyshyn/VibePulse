package com.vpch.vibepulse.domain.service

import com.vpch.vibepulse.domain.model.PaceZone
import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.network.ReccoBeatsApiService
import com.vpch.vibepulse.network.SpotifyApiService
import com.vpch.vibepulse.network.model.ReccoBeatsAudioFeaturesDto
import com.vpch.vibepulse.network.model.TrackDto
import com.vpch.vibepulse.utils.toDomain
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class PlaylistGenerator(
    private val spotifyApiService: SpotifyApiService,
    private val reccoBeatsApiService: ReccoBeatsApiService
) {
    private val playedUris = mutableSetOf<String>()

    private val spamKeywords = listOf(
        "ringtone", "white noise", "lullaby", "8d audio"
    )

    private data class Candidate(
        val track: Track,
        val popularity: Int
    )

    private data class ScoredTrack(
        val track: Track,
        val score: Double,
        val isInstrumental: Boolean,
        val hasFeatures: Boolean
    )

    suspend fun pickTracks(
        zone: PaceZone,
        averageCadence: Int,
        weather: WeatherSnapshot?,
        preferences: PlaylistPreferences?,
        count: Int = 3
    ): List<Track> {
        val selectedGenres = resolveGenres(zone, weather, preferences)
        val yearWindow = listOf("year:2023-2026", "year:2019-2022", "year:2015-2018").random()

        val collectedDtos = mutableListOf<TrackDto>()
        for (genre in selectedGenres) {
            val query = "genre:\"$genre\" $yearWindow"
            val batch = spotifyApiService.searchTracks(
                query = query,
                limit = 40,
                offset = 0
            ).getOrNull().orEmpty()
            collectedDtos.addAll(batch)
            if (collectedDtos.size >= 50) break
        }

        val cleanDtos = collectedDtos
            .distinctBy { it.id }
            .filter { it.isPlayable && !isSpamTrack(it) }

        val popularDtos = cleanDtos
            .filter { it.popularity >= 55 }
            .ifEmpty { cleanDtos.filter { it.popularity >= 45 } }
            .ifEmpty { cleanDtos }

        val primaryGenreLabel = selectedGenres.firstOrNull().orEmpty()

        val allowedCandidates = popularDtos
            .map { dto ->
                Candidate(
                    track = dto.toDomain().copy(
                        tempo = averageCadence,
                        genre = primaryGenreLabel
                    ),
                    popularity = dto.popularity
                )
            }
            .filter { candidate -> preferences?.isTrackAllowed(candidate.track) ?: true }

        val freshCandidates = allowedCandidates
            .filterNot { it.track.spotifyUri in playedUris }
            .ifEmpty { allowedCandidates }

        if (freshCandidates.isEmpty()) return emptyList()

        val candidatesForAnalysis = freshCandidates
            .sortedByDescending { it.popularity }
            .take(40)

        val trackIds = candidatesForAnalysis.map { it.track.spotifyUri.substringAfterLast(":") }
        val featuresById = reccoBeatsApiService.getAudioFeatures(trackIds)
            .getOrDefault(emptyMap())

        val analyzedTracks = candidatesForAnalysis.map { candidate ->
            val spotifyId = candidate.track.spotifyUri.substringAfterLast(":")
            val features = featuresById[spotifyId]
            val realTempo = features?.tempo?.roundToInt()?.takeIf { it > 0 } ?: candidate.track.tempo
            val enrichedTrack = candidate.track.copy(tempo = realTempo)

            val isInstrumental = (features?.instrumentalness ?: 0.0) > 0.25

            val score = scoreCandidate(
                features = features,
                popularity = candidate.popularity,
                targetCadence = averageCadence,
                zone = zone,
                weather = weather,
                weatherEnabled = preferences?.weatherEnabled != false
            )

            ScoredTrack(
                track = enrichedTrack,
                score = score,
                isInstrumental = isInstrumental,
                hasFeatures = features != null
            )
        }

        val vocalTracks = analyzedTracks
            .filter { !it.isInstrumental && it.hasFeatures }
            .ifEmpty { analyzedTracks.filter { !it.isInstrumental } }
            .ifEmpty { analyzedTracks }
            .sortedByDescending { it.score }
            .map { it.track }

        val topPoolSize = maxOf(count * 3, 8)
        val selected = vocalTracks
            .take(topPoolSize)
            .shuffled()
            .take(count)

        selected.forEach { playedUris.add(it.spotifyUri) }
        return selected
    }

    fun resetHistory() {
        playedUris.clear()
    }

    private fun resolveGenres(
        zone: PaceZone,
        weather: WeatherSnapshot?,
        preferences: PlaylistPreferences?
    ): List<String> {
        val userGenres = preferences?.genres?.filter { it.isNotBlank() }.orEmpty()
        if (userGenres.isNotEmpty()) {
            return userGenres.shuffled().take(2)
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

    private fun isSpamTrack(dto: TrackDto): Boolean {
        val titleLower = dto.name.lowercase()
        val artistsLower = dto.artists.joinToString(" ") { it.name.lowercase() }
        return spamKeywords.any { keyword ->
            titleLower.contains(keyword) || artistsLower.contains(keyword)
        }
    }

    private fun scoreCandidate(
        features: ReccoBeatsAudioFeaturesDto?,
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
        features: ReccoBeatsAudioFeaturesDto,
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
}