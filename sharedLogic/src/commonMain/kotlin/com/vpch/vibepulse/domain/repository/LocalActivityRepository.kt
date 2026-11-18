package com.vpch.vibepulse.domain.repository

import com.vpch.vibepulse.domain.model.Activity
import com.vpch.vibepulse.domain.model.ActivityDetails
import com.vpch.vibepulse.domain.model.ActivityTrack
import com.vpch.vibepulse.domain.model.User
import kotlinx.coroutines.flow.Flow

interface LocalActivityRepository {
    suspend fun saveActivity(
        user: User,
        activity: Activity,
        tracks: List<ActivityTrack> = emptyList()
    ): Result<Long>

    fun observeActivities(spotifyId: String): Flow<List<Activity>>
    suspend fun getActivityDetails(activityId: Long): ActivityDetails?
    suspend fun deleteActivity(activityId: Long)
}