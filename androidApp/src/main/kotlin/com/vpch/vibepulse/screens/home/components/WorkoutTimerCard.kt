package com.vpch.vibepulse.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpch.vibepulse.presentation.home.RunStatus
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.VibePulseTheme
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText
import com.vpch.vibepulse.ui.mapper.toTimerStatusLabel

@Composable
fun WorkoutTimerCard(
    runStatus: RunStatus,
    elapsedSeconds: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = AppTheme.colors.buttonPrimary,
        border = BorderStroke(
            width = 1.5.dp,
            color = AppTheme.colors.strokePrimary.copy(alpha = 0.55f)
        ),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TitleText(
                text = formatElapsedTime(elapsedSeconds),
                color = AppTheme.colors.textPrimary,
                fontSize = 40.sp,
                lineHeight = 42.sp,
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

private fun formatElapsedTime(totalSeconds: Long): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0L)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60

    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

@PreviewLightDark
@Composable
private fun WorkoutTimerCardPreview() {
    VibePulseTheme {
        Surface(
            color = AppTheme.colors.background,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WorkoutTimerCard(
                    runStatus = RunStatus.IDLE,
                    elapsedSeconds = 0L
                )
                WorkoutTimerCard(
                    runStatus = RunStatus.RUNNING,
                    elapsedSeconds = 1543L
                )
                WorkoutTimerCard(
                    runStatus = RunStatus.PAUSED,
                    elapsedSeconds = 3605L
                )
            }
        }
    }
}