package com.vpch.vibepulse.screens.statistics.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vpch.vibepulse.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Instant

internal fun formatCompactSteps(steps: Int): String =
    if (steps >= 1000) "${(steps / 100) / 10.0}k" else steps.toString()

internal fun formatNumberWithSpaces(number: Int): String =
    number.toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

@Composable
internal fun formatDurationReadable(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        stringResource(R.string.stats_duration_hours_mins, hours.toInt(), minutes.toInt())
    } else {
        stringResource(R.string.stats_duration_mins_moving, minutes.toInt())
    }
}

internal fun formatShortDay(epochMs: Long): String =
    SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(epochMs))

internal fun formatFullDateTime(instant: Instant): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("uk")).format(Date(instant.toEpochMilliseconds()))