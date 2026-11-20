package com.vpch.vibepulse.screens.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.StatisticsSummary
import com.vpch.vibepulse.presentation.statistics.HistoryUiState
import com.vpch.vibepulse.presentation.statistics.OverviewUiState
import com.vpch.vibepulse.presentation.statistics.StatisticsIntent
import com.vpch.vibepulse.presentation.statistics.StatisticsMessage
import com.vpch.vibepulse.presentation.statistics.StatisticsUiState
import com.vpch.vibepulse.presentation.statistics.StatisticsViewModel
import com.vpch.vibepulse.screens.statistics.components.ActivityDetailsSheetContent
import com.vpch.vibepulse.screens.statistics.components.ActivityHistoryCard
import com.vpch.vibepulse.screens.statistics.components.DailyStepsChartCard
import com.vpch.vibepulse.screens.statistics.components.EmptyActivitiesCard
import com.vpch.vibepulse.screens.statistics.components.HistoryHeaderRow
import com.vpch.vibepulse.screens.statistics.components.PeriodSelectorRow
import com.vpch.vibepulse.screens.statistics.components.PremiumPaywallView
import com.vpch.vibepulse.screens.statistics.components.StatisticsDefaults
import com.vpch.vibepulse.screens.statistics.components.SummaryKpiGrid
import com.vpch.vibepulse.screens.statistics.components.formatShortDay
import com.vpch.vibepulse.storage.SubscriptionState
import com.vpch.vibepulse.storage.SubscriptionStatus
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.VibePulsePillBadge
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StatisticsScreen(
    bottomPadding: Dp = 0.dp,
    viewModel: StatisticsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StatisticsContent(
        uiState = uiState,
        bottomPadding = bottomPadding,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsContent(
    modifier: Modifier = Modifier,
    uiState: StatisticsUiState,
    bottomPadding: Dp = 0.dp,
    onIntent: (StatisticsIntent) -> Unit,
) {
    when (uiState) {
        StatisticsUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }

        is StatisticsUiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                BodyText(
                    text = uiState.message.resolve(),
                    color = AppTheme.colors.textError,
                    style = AppTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }

        is StatisticsUiState.Paywall -> {
            PremiumPaywallView(
                subscriptionState = uiState.subscriptionState,
                bottomPadding = bottomPadding,
                onActivateClick = { onIntent(StatisticsIntent.ActivateSubscription) },
                modifier = modifier
            )
        }

        is StatisticsUiState.Success -> {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            uiState.details.selectedDetails?.let { selectedDetails ->
                ModalBottomSheet(
                    onDismissRequest = { onIntent(StatisticsIntent.CloseActivityDetails) },
                    sheetState = sheetState,
                    containerColor = AppTheme.colors.background,
                    contentColor = AppTheme.colors.textPrimary,
                    shape = RoundedCornerShape(
                        topStart = StatisticsDefaults.SheetCornerRadius,
                        topEnd = StatisticsDefaults.SheetCornerRadius
                    )
                ) {
                    ActivityDetailsSheetContent(
                        enriched = selectedDetails,
                        onDelete = { id -> onIntent(StatisticsIntent.DeleteActivity(id)) }
                    )
                }
            }

            Box(modifier = modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = StatisticsDefaults.ScreenHorizontalPadding,
                        end = StatisticsDefaults.ScreenHorizontalPadding,
                        top = StatisticsDefaults.ScreenTopPadding,
                        bottom = 16.dp + bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(StatisticsDefaults.SectionSpacing)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TitleText(
                                text = stringResource(R.string.stats_title),
                                style = AppTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary
                            )

                            val sub = uiState.subscriptionState
                            val endText = sub.endEpochMs?.let { formatShortDay(it) }
                            if (endText != null) {
                                VibePulsePillBadge(
                                    text = stringResource(
                                        if (sub.isTrial) R.string.stats_trial_until else R.string.stats_premium_until,
                                        endText
                                    ),
                                    contentColor = AppTheme.colors.indicator,
                                    borderColor = AppTheme.colors.indicator.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    item {
                        PeriodSelectorRow(
                            selectedPeriod = uiState.overview.selectedPeriod,
                            onSelectPeriod = { onIntent(StatisticsIntent.SelectPeriod(it)) }
                        )
                    }

                    item { SummaryKpiGrid(summary = uiState.overview.summary) }

                    item {
                        DailyStepsChartCard(
                            selectedPeriod = uiState.overview.selectedPeriod,
                            dailyStats = uiState.overview.summary.dailyStats
                        )
                    }

                    item {
                        HistoryHeaderRow(
                            totalCount = uiState.history.totalFilteredCount,
                            selectedSort = uiState.history.sortOrder,
                            onSelectSort = { onIntent(StatisticsIntent.SelectSortOrder(it)) }
                        )
                    }

                    if (uiState.history.activities.isEmpty()) {
                        item { EmptyActivitiesCard() }
                    } else {
                        items(items = uiState.history.activities, key = { it.id }) { activity ->
                            ActivityHistoryCard(
                                activity = activity,
                                onClick = { onIntent(StatisticsIntent.OpenActivityDetails(activity.id)) }
                            )
                        }

                        if (uiState.history.hasMoreActivities) {
                            item {
                                OutlinedButton(
                                    onClick = { onIntent(StatisticsIntent.LoadMoreActivities) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(StatisticsDefaults.SelectorInnerRadius),
                                    border = BorderStroke(
                                        1.dp,
                                        AppTheme.colors.strokePrimary.copy(alpha = 0.6f)
                                    )
                                ) {
                                    BodyText(
                                        text = stringResource(
                                            R.string.stats_load_more,
                                            uiState.history.totalFilteredCount - uiState.history.activities.size
                                        ),
                                        style = AppTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppTheme.colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                if (bottomPadding > 0.dp) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(bottomPadding)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        AppTheme.colors.background.copy(alpha = 0.6f),
                                        AppTheme.colors.background.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsMessage.resolve(): String = when (this) {
    StatisticsMessage.LoadError -> stringResource(R.string.stats_error_load)
    StatisticsMessage.UserIdentifyError -> stringResource(R.string.stats_error_user)
    is StatisticsMessage.Custom -> text
}

@PreviewLightDark
@Composable
private fun StatisticsScreenPreview() = VibePulsePreview(padding = 0.dp) {
    StatisticsContent(
        uiState = StatisticsUiState.Success(
            subscriptionState = SubscriptionState(status = SubscriptionStatus.ACTIVE, isTrial = false),
            overview = OverviewUiState(
                summary = StatisticsSummary(
                    totalSteps = 32400,
                    totalRuns = 4,
                    totalDurationSeconds = 10800L,
                    overallAvgCadence = 168,
                    bestMaxCadence = 182,
                    dailyStats = emptyList()
                )
            ),
            history = HistoryUiState(
                activities = emptyList(),
                totalFilteredCount = 0
            )
        ),
        onIntent = {}
    )
}