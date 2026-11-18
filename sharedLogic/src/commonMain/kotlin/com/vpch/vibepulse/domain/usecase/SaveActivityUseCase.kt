package com.vpch.vibepulse.domain.usecase

import com.vpch.vibepulse.domain.model.Activity
import com.vpch.vibepulse.domain.model.ActivityTrack
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.repository.LocalActivityRepository

class SaveActivityUseCase(
    private val localActivityRepository: LocalActivityRepository
) {
    suspend operator fun invoke(
        user: User,
        activity: Activity,
        activeDurationSeconds: Long,
        tracks: List<ActivityTrack> = emptyList()
    ): Result<Long> {
        val finalizedActivity = activity.calculateStatistics().copy(
            activeDurationSeconds = activeDurationSeconds
        )
        return localActivityRepository.saveActivity(
            user = user,
            activity = finalizedActivity,
            tracks = tracks
        )
    }
}