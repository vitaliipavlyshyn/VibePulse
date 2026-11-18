package com.vpch.vibepulse.domain.model

data class ActivityTrack(
    val spotifyUri: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String?,
    val tempo: Int,
    val avgCadenceDuringTrack: Int
)

data class ActivityDetails(
    val activity: Activity,
    val tracks: List<ActivityTrack>
)

data class DailyStepsStat(
    val dayStartEpochMs: Long,
    val totalSteps: Int,
    val totalDurationSeconds: Long,
    val avgCadence: Int
)

data class StatisticsSummary(
    val totalRuns: Int,
    val totalSteps: Int,
    val totalDurationSeconds: Long,
    val overallAvgCadence: Int,
    val bestMaxCadence: Int,
    val dailyStats: List<DailyStepsStat>
)