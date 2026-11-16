package com.vpch.vibepulse.domain.usecase

import com.vpch.vibepulse.domain.repository.TrackRepository

class ToggleTrackLikeUseCase(
    private val trackRepository: TrackRepository
) {
    suspend operator fun invoke(trackUri: String, targetLiked: Boolean): Result<Unit> {
        val trackId = trackUri.substringAfterLast(":").trim()
        return trackRepository.setTrackSaved(trackId, targetLiked)
    }
}