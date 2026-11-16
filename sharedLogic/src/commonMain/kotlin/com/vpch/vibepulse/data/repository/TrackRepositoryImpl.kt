package com.vpch.vibepulse.data.repository

import com.vpch.vibepulse.domain.model.TrackAudioFeatures
import com.vpch.vibepulse.domain.model.TrackCandidate
import com.vpch.vibepulse.domain.repository.TrackRepository
import com.vpch.vibepulse.network.ReccoBeatsApiService
import com.vpch.vibepulse.network.SpotifyApiService
import com.vpch.vibepulse.network.model.ReccoBeatsAudioFeaturesDto
import com.vpch.vibepulse.utils.toDomain

class TrackRepositoryImpl(
    private val spotifyApiService: SpotifyApiService,
    private val reccoBeatsApiService: ReccoBeatsApiService
) : TrackRepository {

    override suspend fun searchTracks(
        query: String,
        limit: Int,
        offset: Int
    ): Result<List<TrackCandidate>> =
        spotifyApiService.searchTracks(query = query, limit = limit, offset = offset)
            .map { dtos ->
                dtos.map { dto ->
                    TrackCandidate(
                        track = dto.toDomain(),
                        popularity = dto.popularity,
                        isPlayable = dto.isPlayable
                    )
                }
            }

    override suspend fun getAudioFeatures(
        trackIds: List<String>
    ): Result<Map<String, TrackAudioFeatures>> =
    reccoBeatsApiService.getAudioFeatures(trackIds)
    .map { map ->
        map.mapValues { (_, dto) ->
            TrackAudioFeatures(
                tempo = dto.tempo,
                energy = dto.energy,
                danceability = dto.danceability,
                valence = dto.valence,
                instrumentalness = dto.instrumentalness
            )
        }
    }

    override suspend fun isTrackSaved(trackId: String): Result<Boolean> =
        spotifyApiService.isTrackSaved(trackId)

    override suspend fun setTrackSaved(trackId: String, saved: Boolean): Result<Unit> =
        if (saved) {
            spotifyApiService.saveTrack(trackId)
        } else {
            spotifyApiService.removeSavedTrack(trackId)
        }
}