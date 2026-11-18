package com.vpch.vibepulse.screens.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.StatisticsSummary
import com.vpch.vibepulse.domain.usecase.StatisticsPeriod
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.VibePulseCard
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText
import com.vpch.vibepulse.ui.mapper.toTabLabel

@Composable
internal fun PeriodSelectorRow(
    selectedPeriod: StatisticsPeriod,
    onSelectPeriod: (StatisticsPeriod) -> Unit
) {
    VibePulseCard(
        shape = RoundedCornerShape(StatisticsDefaults.SelectorCornerRadius),
        borderColor = AppTheme.colors.strokePrimary.copy(alpha = 0.45f),
        borderWidth = 1.dp,
        elevation = 0.dp,
        contentPadding = PaddingValues(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatisticsPeriod.entries.forEach { period ->
                val isSelected = period == selectedPeriod
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(StatisticsDefaults.SelectorInnerRadius))
                        .background(if (isSelected) AppTheme.colors.buttonFilled else Color.Transparent)
                        .clickable { onSelectPeriod(period) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BodyText(
                        text = period.toTabLabel(),
                        style = AppTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) AppTheme.colors.onButtonFilled else AppTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
internal fun SummaryKpiGrid(summary: StatisticsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(StatisticsDefaults.GridSpacing)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(StatisticsDefaults.GridSpacing)
        ) {
            KpiCard(
                title = stringResource(R.string.stats_kpi_total_steps),
                value = formatNumberWithSpaces(summary.totalSteps),
                subtitle = stringResource(R.string.stats_kpi_selected_period),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = stringResource(R.string.stats_kpi_runs),
                value = summary.totalRuns.toString(),
                subtitle = formatDurationReadable(summary.totalDurationSeconds),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(StatisticsDefaults.GridSpacing)
        ) {
            KpiCard(
                title = stringResource(R.string.stats_kpi_avg_cadence),
                value = summary.overallAvgCadence.toString(),
                subtitle = stringResource(R.string.stats_kpi_spm_unit),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = stringResource(R.string.stats_kpi_record_cadence),
                value = summary.bestMaxCadence.toString(),
                subtitle = stringResource(R.string.stats_kpi_max_spm),
                accent = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    VibePulseCard(
        modifier = modifier,
        shape = RoundedCornerShape(StatisticsDefaults.KpiCardCornerRadius),
        containerColor = AppTheme.colors.buttonSecondary.copy(alpha = 0.55f),
        borderColor = Color.Transparent,
        borderWidth = 0.dp,
        elevation = 0.dp,
        contentPadding = PaddingValues(StatisticsDefaults.CardPadding)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BodyText(
                text = title,
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary
            )
            TitleText(
                text = value,
                style = AppTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (accent) AppTheme.colors.indicator else AppTheme.colors.textPrimary
            )
            BodyText(
                text = subtitle,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun StatisticsSummaryPreview() = VibePulsePreview {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PeriodSelectorRow(selectedPeriod = StatisticsPeriod.WEEK, onSelectPeriod = {})
        SummaryKpiGrid(
            summary = StatisticsSummary(
                totalSteps = 42850,
                totalRuns = 6,
                totalDurationSeconds = 14820L,
                overallAvgCadence = 166,
                bestMaxCadence = 184,
                dailyStats = emptyList()
            )
        )
    }
}