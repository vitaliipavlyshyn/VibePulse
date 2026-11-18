package com.vpch.vibepulse.data.local.mapper

import com.vpch.vibepulse.data.local.entity.ActivityEntity
import com.vpch.vibepulse.data.local.entity.ActivityTrackEntity
import com.vpch.vibepulse.data.local.entity.CadenceSampleEntity
import com.vpch.vibepulse.data.local.entity.UserEntity
import com.vpch.vibepulse.data.local.entity.WeatherSnapshotEntity
import com.vpch.vibepulse.domain.model.Activity
import com.vpch.vibepulse.domain.model.ActivityTrack
import com.vpch.vibepulse.domain.model.CadenceSample
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.WeatherSnapshot
import kotlin.math.roundToInt
import kotlin.time.Instant

fun User.toEntity(): UserEntity = UserEntity(
    spotifyId = spotifyId,
    displayName = displayName,
    avatarUrl = avatarUrl,
    hasPremium = hasPremium
)

fun UserEntity.toDomain(): User = User(
    id = spotifyId,
    displayName = displayName,
    avatarUrl = avatarUrl,
    hasPremium = hasPremium
)

fun WeatherSnapshot.toEntity(fetchedAt: Long): WeatherSnapshotEntity = WeatherSnapshotEntity(
    area = area,
    temperature = temperature,
    condition = condition,
    fetchedAt = fetchedAt
)

fun WeatherSnapshotEntity.toDomain(): WeatherSnapshot = WeatherSnapshot(
    area = area,
    temperature = temperature,
    condition = condition,
    iconUrl = null
)

fun Activity.toEntity(spotifyId: String): ActivityEntity {
    val endMs = endTime?.toEpochMilliseconds() ?: startTime.toEpochMilliseconds()
    val durationSec = if (activeDurationSeconds > 0L) {
        activeDurationSeconds
    } else {
        ((endMs - startTime.toEpochMilliseconds()) / 1000L).coerceAtLeast(0L)
    }

    return ActivityEntity(
        id = id,
        spotifyId = spotifyId,
        weatherId = null,
        startTime = startTime.toEpochMilliseconds(),
        endTime = endMs,
        activeDurationSeconds = durationSec,
        totalSteps = totalSteps,
        avgCadence = avgCadence.roundToInt(),
        maxCadence = maxCadence.roundToInt()
    )
}

fun ActivityEntity.toDomain(
    weather: WeatherSnapshot? = null,
    samples: List<CadenceSample> = emptyList()
): Activity = Activity(
    id = id,
    startTime = Instant.fromEpochMilliseconds(startTime),
    endTime = Instant.fromEpochMilliseconds(endTime),
    activeDurationSeconds = activeDurationSeconds,
    totalSteps = totalSteps,
    avgCadence = avgCadence.toDouble(),
    maxCadence = maxCadence.toDouble(),
    weather = weather,
    samples = samples
)

fun CadenceSample.toEntity(activityId: Long = 0L): CadenceSampleEntity = CadenceSampleEntity(
    activityId = activityId,
    timestamp = timestamp.toEpochMilliseconds(),
    stepsPerMinute = stepsPerMinute,
    trackUri = trackUri
)

fun CadenceSampleEntity.toDomain(): CadenceSample = CadenceSample(
    timestamp = Instant.fromEpochMilliseconds(timestamp),
    stepsPerMinute = stepsPerMinute,
    trackUri = trackUri
)

fun ActivityTrack.toEntity(activityId: Long = 0L): ActivityTrackEntity = ActivityTrackEntity(
    activityId = activityId,
    spotifyUri = spotifyUri,
    title = title,
    artist = artist,
    albumArtUrl = albumArtUrl,
    tempo = tempo,
    avgCadenceDuringTrack = avgCadenceDuringTrack
)

fun ActivityTrackEntity.toDomain(): ActivityTrack = ActivityTrack(
    spotifyUri = spotifyUri,
    title = title,
    artist = artist,
    albumArtUrl = albumArtUrl,
    tempo = tempo,
    avgCadenceDuringTrack = avgCadenceDuringTrack
)