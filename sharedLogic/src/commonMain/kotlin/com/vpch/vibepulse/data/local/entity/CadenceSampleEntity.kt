package com.vpch.vibepulse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cadence_sample",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activity_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["activity_id"])]
)
data class CadenceSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "activity_id")
    val activityId: Long,
    val timestamp: Long,
    @ColumnInfo(name = "steps_per_minute")
    val stepsPerMinute: Int,
    @ColumnInfo(name = "track_uri")
    val trackUri: String? = null
)