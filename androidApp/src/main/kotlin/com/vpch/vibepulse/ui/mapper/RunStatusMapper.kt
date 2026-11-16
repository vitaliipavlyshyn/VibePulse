package com.vpch.vibepulse.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vpch.vibepulse.R
import com.vpch.vibepulse.presentation.home.RunStatus

@Composable
fun RunStatus.toTimerStatusLabel(): String = stringResource(
    when (this) {
        RunStatus.IDLE -> R.string.timer_status_idle
        RunStatus.RUNNING -> R.string.timer_status_running
        RunStatus.PAUSED -> R.string.timer_status_paused
    }
)

@Composable
fun RunStatus.toSubtitleLabel(): String = stringResource(
    when (this) {
        RunStatus.IDLE -> R.string.run_status_subtitle_idle
        RunStatus.RUNNING -> R.string.run_status_subtitle_running
        RunStatus.PAUSED -> R.string.run_status_subtitle_paused
    }
)