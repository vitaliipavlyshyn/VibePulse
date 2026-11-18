package com.vpch.vibepulse.presentation.statistics

import com.vpch.vibepulse.domain.model.Activity
import com.vpch.vibepulse.domain.model.StatisticsSummary
import com.vpch.vibepulse.domain.usecase.EnrichedActivityDetails
import com.vpch.vibepulse.domain.usecase.StatisticsPeriod
import com.vpch.vibepulse.storage.SubscriptionState

enum class ActivitySortOrder(val title: String) {
    NEWEST_FIRST("Спочатку новіші"),
    OLDEST_FIRST("Спочатку старіші"),
    MOST_STEPS("Найбільше кроків"),
    HIGHEST_CADENCE("Найвищий каденс")
}

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState

    data class Paywall(
        val subscriptionState: SubscriptionState
    ) : StatisticsUiState

    data class Success(
        val subscriptionState: SubscriptionState,
        val selectedPeriod: StatisticsPeriod = StatisticsPeriod.WEEK,
        val sortOrder: ActivitySortOrder = ActivitySortOrder.NEWEST_FIRST,
        val summary: StatisticsSummary = StatisticsSummary(
            totalRuns = 0,
            totalSteps = 0,
            totalDurationSeconds = 0L,
            overallAvgCadence = 0,
            bestMaxCadence = 0,
            dailyStats = emptyList()
        ),
        val activities: List<Activity> = emptyList(),
        val totalFilteredCount: Int = 0,
        val hasMoreActivities: Boolean = false,
        val selectedActivityDetails: EnrichedActivityDetails? = null,
        val isDetailsLoading: Boolean = false
    ) : StatisticsUiState

    data class Error(val message: String) : StatisticsUiState
}

sealed interface StatisticsIntent {
    data object ActivateSubscription : StatisticsIntent
    data class SelectPeriod(val period: StatisticsPeriod) : StatisticsIntent
    data class SelectSortOrder(val sortOrder: ActivitySortOrder) : StatisticsIntent
    data object LoadMoreActivities : StatisticsIntent
    data class OpenActivityDetails(val activityId: Long) : StatisticsIntent
    data object CloseActivityDetails : StatisticsIntent
    data class DeleteActivity(val activityId: Long) : StatisticsIntent
}