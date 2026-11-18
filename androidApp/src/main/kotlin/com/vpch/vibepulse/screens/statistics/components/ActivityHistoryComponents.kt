package com.vpch.vibepulse.screens.statistics.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.Activity
import com.vpch.vibepulse.presentation.statistics.ActivitySortOrder
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.VibePulseCard
import com.vpch.vibepulse.ui.components.VibePulsePillBadge
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText
import com.vpch.vibepulse.ui.mapper.toLabel
import com.vpch.vibepulse.utils.formatSecondsToClock
import kotlin.math.roundToInt

@Composable
internal fun HistoryHeaderRow(
    totalCount: Int,
    selectedSort: ActivitySortOrder,
    onSelectSort: (ActivitySortOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            TitleText(
                text = stringResource(R.string.stats_history_title),
                style = AppTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
            BodyText(
                text = stringResource(R.string.stats_history_count, totalCount),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary
            )
        }

        Box {
            VibePulsePillBadge(
                text = "${selectedSort.toLabel()} ▾",
                containerColor = AppTheme.colors.buttonPrimary,
                borderColor = AppTheme.colors.strokePrimary.copy(alpha = 0.5f),
                textStyle = AppTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                onClick = { expanded = true }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = AppTheme.colors.background
            ) {
                ActivitySortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = {
                            BodyText(
                                text = order.toLabel(),
                                fontWeight = if (order == selectedSort) FontWeight.Bold else FontWeight.Normal,
                                color = if (order == selectedSort) AppTheme.colors.indicator else AppTheme.colors.textPrimary
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelectSort(order)
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ActivityHistoryCard(
    activity: Activity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(StatisticsDefaults.HistoryCardCornerRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(StatisticsDefaults.HistoryCardCornerRadius),
        color = AppTheme.colors.buttonPrimary,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, AppTheme.colors.strokePrimary.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(StatisticsDefaults.CardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TitleText(
                        text = formatFullDateTime(activity.startTime),
                        style = AppTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )

                    activity.weather?.let { w ->
                        VibePulsePillBadge(
                            text = stringResource(
                                R.string.stats_weather_chip,
                                w.temperature.roundToInt(),
                                w.condition
                            ),
                            contentColor = AppTheme.colors.textSecondary,
                            borderColor = null,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                BodyText(
                    text = stringResource(R.string.stats_history_details_action),
                    style = AppTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.indicator
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricMiniItem(
                    label = stringResource(R.string.stats_metric_steps),
                    value = formatNumberWithSpaces(activity.totalSteps)
                )
                MetricMiniItem(
                    label = stringResource(R.string.stats_metric_time),
                    value = formatSecondsToClock(activity.activeDurationSeconds)
                )
                MetricMiniItem(
                    label = stringResource(R.string.stats_metric_avg_cadence),
                    value = stringResource(R.string.stats_spm_value, activity.avgCadence.roundToInt())
                )
                MetricMiniItem(
                    label = stringResource(R.string.stats_metric_max_cadence),
                    value = activity.maxCadence.roundToInt().toString()
                )
            }
        }
    }
}

@Composable
private fun MetricMiniItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        BodyText(
            text = label,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.textSecondary
        )
        BodyText(
            text = value,
            style = AppTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary
        )
    }
}

@Composable
internal fun EmptyActivitiesCard() {
    VibePulseCard(
        shape = RoundedCornerShape(24.dp),
        containerColor = AppTheme.colors.buttonSecondary.copy(alpha = 0.55f),
        borderColor = Color.Transparent,
        borderWidth = 0.dp,
        elevation = 0.dp,
        contentPadding = PaddingValues(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TitleText(
                text = stringResource(R.string.stats_empty_title),
                style = AppTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            BodyText(
                text = stringResource(R.string.stats_empty_subtitle),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun EmptyActivitiesCardPreview() = VibePulsePreview {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HistoryHeaderRow(totalCount = 12, selectedSort = ActivitySortOrder.NEWEST_FIRST, onSelectSort = {})
        EmptyActivitiesCard()
    }
}