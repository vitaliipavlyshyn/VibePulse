package com.vpch.vibepulse.data.repository.local

import com.vpch.vibepulse.data.local.dao.ActivityDao
import com.vpch.vibepulse.data.local.mapper.toDomain
import com.vpch.vibepulse.data.local.mapper.toEntity
import com.vpch.vibepulse.domain.model.Activity
import com.vpch.vibepulse.domain.model.ActivityDetails
import com.vpch.vibepulse.domain.model.ActivityTrack
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.repository.LocalActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class LocalActivityRepositoryImpl(
    private val activityDao: ActivityDao
) : LocalActivityRepository {

    override suspend fun saveActivity(
        user: User,
        activity: Activity,
        tracks: List<ActivityTrack>
    ): Result<Long> = runCatching {
        val fetchedAt = Clock.System.now().toEpochMilliseconds()
        val userEntity = user.toEntity()
        val weatherEntity = activity.weather?.toEntity(fetchedAt = fetchedAt)
        val activityEntity = activity.toEntity(spotifyId = userEntity.spotifyId)
        val sampleEntities = activity.samples.map { it.toEntity() }
        val trackEntities = tracks.map { it.toEntity() }

        activityDao.saveCompleteWorkout(
            user = userEntity,
            weather = weatherEntity,
            activity = activityEntity,
            samples = sampleEntities,
            tracks = trackEntities
        )
    }

    override fun observeActivities(spotifyId: String): Flow<List<Activity>> {
        return activityDao.observeActivitiesByUser(spotifyId).map { entities ->
            entities.map { entity ->
                val weather = entity.weatherId?.let { weatherId ->
                    activityDao.getWeatherById(weatherId)?.toDomain()
                }
                entity.toDomain(weather = weather)
            }
        }
    }

    override suspend fun getActivityDetails(activityId: Long): ActivityDetails? {
        val activityEntity = activityDao.getActivityById(activityId) ?: return null
        val weather = activityEntity.weatherId?.let { weatherId ->
            activityDao.getWeatherById(weatherId)?.toDomain()
        }
        val samples = activityDao.getCadenceSamplesForActivity(activityId)
            .map { it.toDomain() }
        val tracks = activityDao.getTracksForActivity(activityId)
            .map { it.toDomain() }

        return ActivityDetails(
            activity = activityEntity.toDomain(
                weather = weather,
                samples = samples
            ),
            tracks = tracks
        )
    }

    override suspend fun deleteActivity(activityId: Long) {
        activityDao.deleteActivityById(activityId)
    }
}