package com.vpch.vibepulse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vpch.vibepulse.data.local.entity.ActivityEntity
import com.vpch.vibepulse.data.local.entity.ActivityTrackEntity
import com.vpch.vibepulse.data.local.entity.CadenceSampleEntity
import com.vpch.vibepulse.data.local.entity.UserEntity
import com.vpch.vibepulse.data.local.entity.WeatherSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherSnapshot(weather: WeatherSnapshotEntity): Long

    @Insert
    suspend fun insertActivity(activity: ActivityEntity): Long

    @Insert
    suspend fun insertCadenceSamples(samples: List<CadenceSampleEntity>)

    @Insert
    suspend fun insertActivityTracks(tracks: List<ActivityTrackEntity>)

    @Transaction
    suspend fun saveCompleteWorkout(
        user: UserEntity,
        weather: WeatherSnapshotEntity?,
        activity: ActivityEntity,
        samples: List<CadenceSampleEntity>,
        tracks: List<ActivityTrackEntity>
    ): Long {
        upsertUser(user)
        val weatherId = weather?.let { insertWeatherSnapshot(it) }
        val activityId = insertActivity(activity.copy(weatherId = weatherId))
        if (samples.isNotEmpty()) {
            insertCadenceSamples(samples.map { it.copy(activityId = activityId) })
        }
        if (tracks.isNotEmpty()) {
            insertActivityTracks(tracks.map { it.copy(activityId = activityId) })
        }
        return activityId
    }

    @Query("SELECT * FROM activity WHERE spotify_id = :spotifyId ORDER BY start_time DESC")
    fun observeActivitiesByUser(spotifyId: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM cadence_sample WHERE activity_id = :activityId ORDER BY timestamp ASC")
    suspend fun getCadenceSamplesForActivity(activityId: Long): List<CadenceSampleEntity>

    @Query("SELECT * FROM activity_track WHERE activity_id = :activityId")
    suspend fun getTracksForActivity(activityId: Long): List<ActivityTrackEntity>

    @Query("SELECT * FROM weather_snapshot WHERE id = :weatherId LIMIT 1")
    suspend fun getWeatherById(weatherId: Long): WeatherSnapshotEntity?

    @Query("DELETE FROM activity WHERE id = :activityId")
    suspend fun deleteActivityById(activityId: Long)

    @Query("SELECT * FROM user WHERE spotify_id = :spotifyId LIMIT 1")
    suspend fun getUserById(spotifyId: String): UserEntity?

    @Query("SELECT * FROM activity WHERE id = :activityId LIMIT 1")
    suspend fun getActivityById(activityId: Long): ActivityEntity?
}