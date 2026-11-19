package com.vpch.vibepulse.domain.usecase

import com.vpch.vibepulse.domain.model.PaceZone
import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.domain.model.TrackCandidate
import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.domain.repository.TrackRepository
import com.vpch.vibepulse.domain.service.TrackScoringEngine
import kotlin.math.roundToInt

class GenerateActivityPlaylistUseCase(
    private val trackRepository: TrackRepository,
    private val scoringEngine: TrackScoringEngine
) {
    private val playedUris = LinkedHashSet<String>()

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
        count: Int = 2
    ): List<Track> {
        val selectedGenres = scoringEngine.selectGenres(zone, weather, preferences)
        val yearWindow = listOf("year:2023-2026", "year:2019-2022", "year:2015-2018").random()

        val collected = mutableListOf<TrackCandidate>()
        for (genre in selectedGenres) {
            val batch = trackRepository.searchTracks(
                query = "genre:\"$genre\" $yearWindow",
                limit = 40,
                offset = listOf(0, 15, 30, 50).random()
            ).getOrNull().orEmpty()
            collected.addAll(batch)
            if (collected.size >= 60) break
        }

        val clean = collected
            .distinctBy { it.track.spotifyUri }
            .filter { it.isPlayable && !scoringEngine.isSpam(it) }

        val popular = clean
            .filter { it.popularity >= 50 }
            .ifEmpty { clean.filter { it.popularity >= 40 } }
            .ifEmpty { clean }

        val primaryGenre = selectedGenres.firstOrNull().orEmpty()
        val fallbackTempo = (zone.minBpm + zone.maxBpm) / 2

        val allowed = popular
            .map { candidate ->
                candidate.copy(
                    track = candidate.track.copy(
                        tempo = fallbackTempo,
                        genre = primaryGenre
                    )
                )
            }
            .filter { candidate -> preferences?.isTrackAllowed(candidate.track) ?: true }

        val fresh = allowed
            .filterNot { it.track.spotifyUri in playedUris }
            .ifEmpty {
                val recent = playedUris.toList().takeLast(15)
                playedUris.clear()
                playedUris.addAll(recent)
                allowed.filterNot { it.track.spotifyUri in playedUris }.ifEmpty { allowed }
            }

        if (fresh.isEmpty()) return emptyList()

        val sampleCandidates = fresh
            .shuffled()
            .take(40)

        val trackIds = sampleCandidates.map { it.track.spotifyUri.substringAfterLast(":") }
        val featuresById = trackRepository.getAudioFeatures(trackIds).getOrDefault(emptyMap())

        val analyzed = sampleCandidates.map { candidate ->
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
            .take(maxOf(count * 4, 8))
            .shuffled()
            .take(count)

        selected.forEach { playedUris.add(it.spotifyUri) }
        return selected
    }

    fun resetHistory() {
        playedUris.clear()
    }
}