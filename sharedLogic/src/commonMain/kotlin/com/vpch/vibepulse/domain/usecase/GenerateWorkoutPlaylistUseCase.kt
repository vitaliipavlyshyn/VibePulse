package com.vpch.vibepulse.domain.usecase

import com.vpch.vibepulse.domain.model.PaceZone
import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.domain.model.TrackCandidate
import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.domain.repository.TrackRepository
import com.vpch.vibepulse.domain.service.TrackScoringEngine
import kotlin.math.roundToInt

class GenerateWorkoutPlaylistUseCase(
    private val trackRepository: TrackRepository,
    private val scoringEngine: TrackScoringEngine
) {
    private val playedUris = mutableSetOf<String>()

    private data class ScoredTrack(
        val track: Track,
        val score: Double,
        val isInstrumental: Boolean,
        val hasFeatures: Boolean
    )

    suspend operator fun invoke(
        zone: PaceZone,
        averageCadence: Int,
        weather: WeatherSnapshot?,
        preferences: PlaylistPreferences?,
        count: Int = 3
    ): List<Track> {
        val selectedGenres = scoringEngine.selectGenres(zone, weather, preferences)
        val yearWindow = listOf("year:2023-2026", "year:2019-2022", "year:2015-2018").random()

        val collected = mutableListOf<TrackCandidate>()
        for (genre in selectedGenres) {
            val batch = trackRepository.searchTracks(
                query = "genre:\"$genre\" $yearWindow",
                limit = 40,
                offset = 0
            ).getOrNull().orEmpty()
            collected.addAll(batch)
            if (collected.size >= 50) break
        }

        val clean = collected
            .distinctBy { it.track.spotifyUri }
            .filter { it.isPlayable && !scoringEngine.isSpam(it) }

        val popular = clean
            .filter { it.popularity >= 55 }
            .ifEmpty { clean.filter { it.popularity >= 45 } }
            .ifEmpty { clean }

        val primaryGenre = selectedGenres.firstOrNull().orEmpty()

        val allowed = popular
            .map { candidate ->
                candidate.copy(
                    track = candidate.track.copy(
                        tempo = averageCadence,
                        genre = primaryGenre
                    )
                )
            }
            .filter { candidate -> preferences?.isTrackAllowed(candidate.track) ?: true }

        val fresh = allowed
            .filterNot { it.track.spotifyUri in playedUris }
            .ifEmpty { allowed }

        if (fresh.isEmpty()) return emptyList()

        val topCandidates = fresh
            .sortedByDescending { it.popularity }
            .take(40)

        val trackIds = topCandidates.map { it.track.spotifyUri.substringAfterLast(":") }
        val featuresById = trackRepository.getAudioFeatures(trackIds).getOrDefault(emptyMap())

        val analyzed = topCandidates.map { candidate ->
            val spotifyId = candidate.track.spotifyUri.substringAfterLast(":")
            val features = featuresById[spotifyId]
            val realTempo = features?.tempo?.roundToInt()?.takeIf { it > 0 } ?: candidate.track.tempo
            val enrichedTrack = candidate.track.copy(tempo = realTempo)

            ScoredTrack(
                track = enrichedTrack,
                score = scoringEngine.score(
                    features = features,
                    popularity = candidate.popularity,
                    targetCadence = averageCadence,
                    zone = zone,
                    weather = weather,
                    weatherEnabled = preferences?.weatherEnabled != false
                ),
                isInstrumental = (features?.instrumentalness ?: 0.0) > 0.25,
                hasFeatures = features != null
            )
        }

        val ranked = analyzed
            .filter { !it.isInstrumental && it.hasFeatures }
            .ifEmpty { analyzed.filter { !it.isInstrumental } }
            .ifEmpty { analyzed }
            .sortedByDescending { it.score }
            .map { it.track }

        val selected = ranked
            .take(maxOf(count * 3, 8))
            .shuffled()
            .take(count)

        selected.forEach { playedUris.add(it.spotifyUri) }
        return selected
    }

    fun resetHistory() {
        playedUris.clear()
    }
}