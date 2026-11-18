package com.vpch.vibepulse.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.usecase.StatisticsPeriod
import com.vpch.vibepulse.presentation.statistics.ActivitySortOrder

@Composable
fun StatisticsPeriod.toTabLabel(): String = stringResource(
    when (this) {
        StatisticsPeriod.WEEK -> R.string.stats_period_week
        StatisticsPeriod.MONTH -> R.string.stats_period_month
        StatisticsPeriod.ALL_TIME -> R.string.stats_period_all_time
    }
)

@Composable
fun StatisticsPeriod.toChartTitle(): String = stringResource(
    when (this) {
        StatisticsPeriod.WEEK -> R.string.stats_chart_title_week
        StatisticsPeriod.MONTH -> R.string.stats_chart_title_month
        StatisticsPeriod.ALL_TIME -> R.string.stats_chart_title_all_time
    }
)

@Composable
fun ActivitySortOrder.toLabel(): String = stringResource(
    when (this) {
        ActivitySortOrder.NEWEST_FIRST -> R.string.stats_sort_newest
        ActivitySortOrder.OLDEST_FIRST -> R.string.stats_sort_oldest
        ActivitySortOrder.MOST_STEPS -> R.string.stats_sort_most_steps
        ActivitySortOrder.HIGHEST_CADENCE -> R.string.stats_sort_highest_cadence
    }
)