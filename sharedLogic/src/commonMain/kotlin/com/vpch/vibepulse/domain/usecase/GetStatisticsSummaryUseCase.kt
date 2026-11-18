package com.vpch.vibepulse.domain.usecase

import com.vpch.vibepulse.domain.model.Activity
import com.vpch.vibepulse.domain.model.DailyStepsStat
import com.vpch.vibepulse.domain.model.StatisticsSummary
import com.vpch.vibepulse.domain.repository.LocalActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

enum class StatisticsPeriod {
    WEEK,
    MONTH,
    ALL_TIME
}

data class PeriodStatisticsResult(
    val summary: StatisticsSummary,
    val filteredActivities: List<Activity>
)

class GetStatisticsSummaryUseCase(
    private val localActivityRepository: LocalActivityRepository
) {
    operator fun invoke(
        spotifyId: String,
        period: StatisticsPeriod = StatisticsPeriod.WEEK
    ): Flow<PeriodStatisticsResult> {
        return localActivityRepository.observeActivities(spotifyId).map { activities ->
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val cutoffMs = when (period) {
                StatisticsPeriod.WEEK -> nowMs - 7.days.inWholeMilliseconds
                StatisticsPeriod.MONTH -> nowMs - 30.days.inWholeMilliseconds
                StatisticsPeriod.ALL_TIME -> 0L
            }

            val filtered = activities.filter { it.startTime.toEpochMilliseconds() >= cutoffMs }

            if (filtered.isEmpty()) {
                return@map PeriodStatisticsResult(
                    summary = StatisticsSummary(
                        totalRuns = 0,
                        totalSteps = 0,
                        totalDurationSeconds = 0L,
                        overallAvgCadence = 0,
                        bestMaxCadence = 0,
                        dailyStats = emptyList()
                    ),
                    filteredActivities = emptyList()
                )
            }

            val totalSteps = filtered.sumOf { it.totalSteps }
            val totalDurationSeconds = filtered.sumOf { it.activeDurationSeconds }
            val validCadences = filtered.map { it.avgCadence }.filter { it > 0.0 }
            val overallAvgCadence = if (validCadences.isNotEmpty()) {
                validCadences.average().roundToInt()
            } else {
                0
            }
            val bestMaxCadence = filtered.maxOfOrNull { it.maxCadence.roundToInt() } ?: 0

            val dayMillis = 1.days.inWholeMilliseconds
            val dailyStats = filtered
                .groupBy { (it.startTime.toEpochMilliseconds() / dayMillis) * dayMillis }
                .map { (dayStartMs, dayActivities) ->
                    val dayAvg = dayActivities
                        .map { it.avgCadence }
                        .filter { it > 0.0 }
                        .takeIf { it.isNotEmpty() }
                        ?.average()
                        ?.roundToInt() ?: 0

                    DailyStepsStat(
                        dayStartEpochMs = dayStartMs,
                        totalSteps = dayActivities.sumOf { it.totalSteps },
                        totalDurationSeconds = dayActivities.sumOf { it.activeDurationSeconds },
                        avgCadence = dayAvg
                    )
                }
                .sortedBy { it.dayStartEpochMs }

            PeriodStatisticsResult(
                summary = StatisticsSummary(
                    totalRuns = filtered.size,
                    totalSteps = totalSteps,
                    totalDurationSeconds = totalDurationSeconds,
                    overallAvgCadence = overallAvgCadence,
                    bestMaxCadence = bestMaxCadence,
                    dailyStats = dailyStats
                ),
                filteredActivities = filtered
            )
        }
    }
}