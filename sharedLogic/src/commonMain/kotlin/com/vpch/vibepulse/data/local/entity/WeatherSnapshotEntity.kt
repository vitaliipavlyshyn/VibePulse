package com.vpch.vibepulse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_snapshot")
data class WeatherSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val area: String,
    val temperature: Double,
    val condition: String,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long
)