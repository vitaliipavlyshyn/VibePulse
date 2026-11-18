package com.vpch.vibepulse.domain.usecase

import com.vpch.vibepulse.domain.model.ActivityDetails
import com.vpch.vibepulse.domain.model.ActivityTrack
import com.vpch.vibepulse.domain.repository.LocalActivityRepository

data class EnrichedActivityDetails(
    val details: ActivityDetails,
    val fastestTrack: ActivityTrack?
)

class GetActivityDetailsUseCase(
    private val localActivityRepository: LocalActivityRepository
) {
    suspend operator fun invoke(activityId: Long): EnrichedActivityDetails? {
        val details = localActivityRepository.getActivityDetails(activityId) ?: return null
        val fastestTrack = details.tracks
            .filter { it.avgCadenceDuringTrack > 0 }
            .maxByOrNull { it.avgCadenceDuringTrack }

        return EnrichedActivityDetails(
            details = details,
            fastestTrack = fastestTrack
        )
    }
}