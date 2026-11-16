package com.vpch.vibepulse.domain.repository

import com.vpch.vibepulse.domain.model.TrackAudioFeatures
import com.vpch.vibepulse.domain.model.TrackCandidate

interface TrackRepository {
    suspend fun searchTracks(query: String, limit: Int = 40, offset: Int = 0): Result<List<TrackCandidate>>
    suspend fun getAudioFeatures(trackIds: List<String>): Result<Map<String, TrackAudioFeatures>>
    suspend fun isTrackSaved(trackId: String): Result<Boolean>
    suspend fun setTrackSaved(trackId: String, saved: Boolean): Result<Unit>
}