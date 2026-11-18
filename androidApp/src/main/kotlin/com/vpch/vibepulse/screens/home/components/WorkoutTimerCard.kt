package com.vpch.vibepulse.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.presentation.home.RunStatus
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.VibePulseCard
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText
import com.vpch.vibepulse.ui.mapper.toTimerStatusLabel
import com.vpch.vibepulse.utils.formatSecondsToClock

@Composable
internal fun WorkoutTimerCard(
    runStatus: RunStatus,
    elapsedSeconds: Long,
    modifier: Modifier = Modifier
) {
    VibePulseCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TitleText(
                text = formatSecondsToClock(elapsedSeconds),
                color = AppTheme.colors.textPrimary,
                fontSize = HomeDefaults.TimerValueFontSize,
                lineHeight = HomeDefaults.TimerValueLineHeight,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            BodyText(
                text = runStatus.toTimerStatusLabel(),
                color = AppTheme.colors.textSecondary,
                style = AppTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WorkoutTimerCardPreview() = VibePulsePreview {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WorkoutTimerCard(runStatus = RunStatus.IDLE, elapsedSeconds = 0L)
        WorkoutTimerCard(runStatus = RunStatus.RUNNING, elapsedSeconds = 1543L)
        WorkoutTimerCard(runStatus = RunStatus.PAUSED, elapsedSeconds = 3605L)
    }
}