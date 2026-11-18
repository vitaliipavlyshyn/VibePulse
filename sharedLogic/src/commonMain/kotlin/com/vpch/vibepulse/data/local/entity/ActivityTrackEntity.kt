package com.vpch.vibepulse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_track",
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
data class ActivityTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "activity_id")
    val activityId: Long,
    @ColumnInfo(name = "spotify_uri")
    val spotifyUri: String,
    val title: String,
    val artist: String,
    @ColumnInfo(name = "album_art_url")
    val albumArtUrl: String?,
    val tempo: Int,
    @ColumnInfo(name = "avg_cadence_during_track")
    val avgCadenceDuringTrack: Int
)