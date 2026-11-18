package com.vpch.vibepulse.screens.statistics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.DailyStepsStat
import com.vpch.vibepulse.domain.usecase.StatisticsPeriod
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.VibePulseCard
import com.vpch.vibepulse.ui.components.VibePulsePillBadge
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText
import com.vpch.vibepulse.ui.mapper.toChartTitle
import kotlin.math.roundToInt

@Composable
internal fun DailyStepsChartCard(
    selectedPeriod: StatisticsPeriod,
    dailyStats: List<DailyStepsStat>
) {
    val barColor = AppTheme.colors.indicator
    val trackColor = AppTheme.colors.trackIndicator.copy(alpha = 0.25f)
    val gridColor = AppTheme.colors.strokePrimary.copy(alpha = 0.35f)
    val labelColor = AppTheme.colors.textSecondary

    val displayPoints = remember(dailyStats, selectedPeriod) {
        when (selectedPeriod) {
            StatisticsPeriod.WEEK -> dailyStats.takeLast(StatisticsDefaults.ChartMaxVisibleDaysWithoutScroll)
            StatisticsPeriod.MONTH -> dailyStats.takeLast(StatisticsDefaults.ChartMonthDaysLimit)
            StatisticsPeriod.ALL_TIME -> dailyStats
        }
    }

    var selectedDayEpochMs by remember(displayPoints) {
        mutableStateOf(displayPoints.lastOrNull()?.dayStartEpochMs)
    }
    val selectedStat = remember(displayPoints, selectedDayEpochMs) {
        displayPoints.find { it.dayStartEpochMs == selectedDayEpochMs } ?: displayPoints.lastOrNull()
    }

    VibePulseCard(
        shape = RoundedCornerShape(StatisticsDefaults.ChartCardCornerRadius),
        containerColor = AppTheme.colors.buttonSecondary.copy(alpha = 0.55f),
        borderColor = Color.Transparent,
        borderWidth = 0.dp,
        elevation = 0.dp,
        contentPadding = PaddingValues(StatisticsDefaults.ChartCardPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(StatisticsDefaults.SectionSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TitleText(
                    text = selectedPeriod.toChartTitle(),
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )

                if (selectedStat != null) {
                    val mins = (selectedStat.totalDurationSeconds / 60).coerceAtLeast(1).toInt()
                    VibePulsePillBadge(
                        text = stringResource(
                            R.string.stats_chart_selected_pill,
                            formatShortDay(selectedStat.dayStartEpochMs),
                            formatNumberWithSpaces(selectedStat.totalSteps),
                            mins
                        ),
                        containerColor = AppTheme.colors.buttonPrimary,
                        contentColor = AppTheme.colors.indicator,
                        borderColor = AppTheme.colors.indicator.copy(alpha = 0.5f)
                    )
                }
            }

            if (displayPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BodyText(
                        text = stringResource(R.string.stats_chart_empty),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val maxSteps = remember(displayPoints) {
                    (displayPoints.maxOfOrNull { it.totalSteps } ?: 1).coerceAtLeast(1)
                }
                val midSteps = (maxSteps / 2f).roundToInt()

                val animatedProgress by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = StatisticsDefaults.ChartAnimationDurationMs),
                    label = "chartAnimation"
                )

                val isScrollable = displayPoints.size > StatisticsDefaults.ChartMaxVisibleDaysWithoutScroll
                val horizontalScrollState = rememberScrollState()

                LaunchedEffect(displayPoints) {
                    if (isScrollable) {
                        horizontalScrollState.scrollTo(horizontalScrollState.maxValue)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(StatisticsDefaults.ChartTotalHeight),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .width(StatisticsDefaults.ChartYAxisWidth)
                            .height(StatisticsDefaults.ChartBarAreaHeight),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        listOf(formatCompactSteps(maxSteps), formatCompactSteps(midSteps), "0").forEach { tick ->
                            BodyText(
                                text = tick,
                                style = AppTheme.typography.labelSmall,
                                fontSize = StatisticsDefaults.YAxisFontSize,
                                color = labelColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(StatisticsDefaults.ChartBarAreaHeight)
                        ) {
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            listOf(0f, size.height / 2f, size.height).forEach { yPos ->
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, yPos),
                                    end = Offset(size.width, yPos),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = if (yPos == size.height) null else dashEffect
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isScrollable) Modifier.horizontalScroll(horizontalScrollState)
                                    else Modifier
                                )
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            displayPoints.forEach { stat ->
                                val isSelected = stat.dayStartEpochMs == selectedStat?.dayStartEpochMs
                                val fraction = (stat.totalSteps.toFloat() / maxSteps.toFloat())
                                    .coerceIn(0.06f, 1f) * animatedProgress

                                val columnModifier = if (isScrollable) {
                                    Modifier.width(StatisticsDefaults.ChartScrollColumnWidth)
                                } else {
                                    Modifier.weight(1f)
                                }

                                Column(
                                    modifier = columnModifier
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { selectedDayEpochMs = stat.dayStartEpochMs },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(StatisticsDefaults.ChartBarAreaHeight),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(StatisticsDefaults.ChartBarWidth)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(StatisticsDefaults.ChartBarCornerRadius))
                                                .background(trackColor)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(StatisticsDefaults.ChartBarWidth)
                                                .fillMaxHeight(fraction)
                                                .clip(RoundedCornerShape(StatisticsDefaults.ChartBarCornerRadius))
                                                .background(
                                                    if (isSelected) barColor else barColor.copy(alpha = 0.55f)
                                                )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    BodyText(
                                        text = formatShortDay(stat.dayStartEpochMs),
                                        style = AppTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) barColor else labelColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DailyStepsChartCardPreview() = VibePulsePreview {
    val now = 1794960000000L
    val dayMs = 86_400_000L
    DailyStepsChartCard(
        selectedPeriod = StatisticsPeriod.WEEK,
        dailyStats = listOf(4200, 7800, 5100, 9600, 3200, 11200, 8450).mapIndexed { i, steps ->
            DailyStepsStat(
                dayStartEpochMs = now - (6 - i) * dayMs,
                totalSteps = steps,
                totalDurationSeconds = (steps / 2.7).toLong(),
                avgCadence = 165
            )
        }
    )
}