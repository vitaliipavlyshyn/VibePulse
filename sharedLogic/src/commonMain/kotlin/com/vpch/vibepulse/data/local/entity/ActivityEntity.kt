package com.vpch.vibepulse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["spotify_id"],
            childColumns = ["spotify_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WeatherSnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["weather_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["spotify_id"]),
        Index(value = ["weather_id"])
    ]
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "spotify_id")
    val spotifyId: String,
    @ColumnInfo(name = "weather_id")
    val weatherId: Long?,
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    @ColumnInfo(name = "end_time")
    val endTime: Long,
    @ColumnInfo(name = "active_duration_seconds")
    val activeDurationSeconds: Long,
    @ColumnInfo(name = "total_steps")
    val totalSteps: Int,
    @ColumnInfo(name = "avg_cadence")
    val avgCadence: Int,
    @ColumnInfo(name = "max_cadence")
    val maxCadence: Int
)