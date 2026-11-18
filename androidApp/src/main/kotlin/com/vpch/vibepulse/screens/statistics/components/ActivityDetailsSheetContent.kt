package com.vpch.vibepulse.screens.statistics.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.ActivityTrack
import com.vpch.vibepulse.domain.model.CadenceSample
import com.vpch.vibepulse.domain.usecase.EnrichedActivityDetails
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.OpenSpotifyTrackDialog
import com.vpch.vibepulse.ui.components.VibePulseArtwork
import com.vpch.vibepulse.ui.components.VibePulseCard
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText
import openTrackInSpotifyOrBrowser

@Composable
internal fun ActivityDetailsSheetContent(
    enriched: EnrichedActivityDetails,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    val activity = enriched.details.activity
    val samples = activity.samples
    val tracks = enriched.details.tracks
    val fastestTrack = enriched.fastestTrack

    var trackToOpen by remember { mutableStateOf<ActivityTrack?>(null) }

    trackToOpen?.let { selectedTrack ->
        OpenSpotifyTrackDialog(
            title = selectedTrack.title,
            artist = selectedTrack.artist,
            albumArtUrl = selectedTrack.albumArtUrl,
            cadenceOrTempo = selectedTrack.avgCadenceDuringTrack,
            isTempoBpm = false,
            onConfirm = {
                openTrackInSpotifyOrBrowser(context, selectedTrack.spotifyUri)
                trackToOpen = null
            },
            onDismiss = { trackToOpen = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    TitleText(
                        text = stringResource(R.string.stats_details_title),
                        style = AppTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                    BodyText(
                        text = formatFullDateTime(activity.startTime),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                }

                TextButton(onClick = { onDelete(activity.id) }) {
                    BodyText(
                        text = stringResource(R.string.stats_details_delete),
                        color = AppTheme.colors.textError,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (samples.size >= 2) {
            item { CadenceLineChartCard(samples = samples) }
        }

        if (fastestTrack != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(StatisticsDefaults.KpiCardCornerRadius))
                        .clickable { trackToOpen = fastestTrack },
                    shape = RoundedCornerShape(StatisticsDefaults.KpiCardCornerRadius),
                    color = AppTheme.colors.buttonSecondary,
                    border = BorderStroke(1.dp, AppTheme.colors.indicator.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VibePulseArtwork(
                            imageUrl = fastestTrack.albumArtUrl,
                            contentDescription = null,
                            shape = RoundedCornerShape(StatisticsDefaults.TrackArtCornerRadius),
                            placeholderIconRes = R.drawable.spotify_logo,
                            modifier = Modifier.size(StatisticsDefaults.FastestTrackArtSize)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            BodyText(
                                text = stringResource(R.string.stats_details_fastest_track_badge),
                                style = AppTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.indicator,
                                letterSpacing = 0.6.sp
                            )
                            BodyText(
                                text = fastestTrack.title,
                                style = AppTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            BodyText(
                                text = stringResource(
                                    R.string.stats_details_artist_spm,
                                    fastestTrack.artist,
                                    fastestTrack.avgCadenceDuringTrack
                                ),
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (tracks.isNotEmpty()) {
            item {
                TitleText(
                    text = stringResource(R.string.stats_details_soundtrack_header, tracks.size),
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
            }

            items(tracks) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { trackToOpen = track }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VibePulseArtwork(
                        imageUrl = track.albumArtUrl,
                        contentDescription = null,
                        shape = RoundedCornerShape(StatisticsDefaults.TrackArtCornerRadius),
                        placeholderIconRes = R.drawable.spotify_logo,
                        modifier = Modifier.size(StatisticsDefaults.TrackListArtSize)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        BodyText(
                            text = track.title,
                            style = AppTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        BodyText(
                            text = track.artist,
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (track.avgCadenceDuringTrack > 0) {
                        BodyText(
                            text = stringResource(R.string.stats_spm_value, track.avgCadenceDuringTrack),
                            style = AppTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.indicator
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    color = AppTheme.colors.strokePrimary.copy(alpha = 0.2f)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun CadenceLineChartCard(samples: List<CadenceSample>) {
    val lineColor = AppTheme.colors.indicator
    val fillBrush = Brush.verticalGradient(
        colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.0f))
    )

    VibePulseCard(
        shape = RoundedCornerShape(StatisticsDefaults.KpiCardCornerRadius),
        borderColor = AppTheme.colors.strokePrimary.copy(alpha = 0.45f),
        borderWidth = 1.dp,
        elevation = 0.dp,
        contentPadding = PaddingValues(StatisticsDefaults.CardPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TitleText(
                text = stringResource(R.string.stats_details_cadence_chart_title),
                style = AppTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(StatisticsDefaults.LineChartHeight)
            ) {
                val values = samples.map { it.stepsPerMinute.toFloat() }
                val minVal = (values.minOrNull() ?: 0f).coerceAtLeast(0f)
                val maxVal = (values.maxOrNull() ?: 200f).coerceAtLeast(minVal + 10f)
                val range = maxVal - minVal

                val strokePath = Path()
                val fillPath = Path()

                values.forEachIndexed { index, value ->
                    val x = (index.toFloat() / (values.size - 1).coerceAtLeast(1)) * size.width
                    val normalizedY = (value - minVal) / range
                    val y = size.height - (normalizedY * size.height * 0.85f + size.height * 0.07f)

                    if (index == 0) {
                        strokePath.moveTo(x, y)
                        fillPath.moveTo(x, size.height)
                        fillPath.lineTo(x, y)
                    } else {
                        strokePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(size.width, size.height)
                fillPath.close()

                drawPath(path = fillPath, brush = fillBrush)
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}