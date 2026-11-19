package com.vpch.vibepulse.presentation.statistics

import com.vpch.vibepulse.domain.model.Activity
import com.vpch.vibepulse.domain.model.StatisticsSummary
import com.vpch.vibepulse.domain.usecase.EnrichedActivityDetails
import com.vpch.vibepulse.domain.usecase.StatisticsPeriod
import com.vpch.vibepulse.storage.SubscriptionState

enum class ActivitySortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    MOST_STEPS,
    HIGHEST_CADENCE
}

data class OverviewUiState(
    val selectedPeriod: StatisticsPeriod = StatisticsPeriod.WEEK,
    val summary: StatisticsSummary = StatisticsSummary(
        totalRuns = 0,
        totalSteps = 0,
        totalDurationSeconds = 0L,
        overallAvgCadence = 0,
        bestMaxCadence = 0,
        dailyStats = emptyList()
    )
)

data class HistoryUiState(
    val sortOrder: ActivitySortOrder = ActivitySortOrder.NEWEST_FIRST,
    val activities: List<Activity> = emptyList(),
    val totalFilteredCount: Int = 0,
    val hasMoreActivities: Boolean = false
)

data class ActivityDetailsUiState(
    val selectedDetails: EnrichedActivityDetails? = null,
    val isLoading: Boolean = false
)

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState

    data class Paywall(
        val subscriptionState: SubscriptionState
    ) : StatisticsUiState

    data class Success(
        val subscriptionState: SubscriptionState,
        val overview: OverviewUiState = OverviewUiState(),
        val history: HistoryUiState = HistoryUiState(),
        val details: ActivityDetailsUiState = ActivityDetailsUiState()
    ) : StatisticsUiState

    data class Error(val message: StatisticsMessage) : StatisticsUiState
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

sealed interface StatisticsEffect {
    data class ShowMessage(val message: StatisticsMessage) : StatisticsEffect
}