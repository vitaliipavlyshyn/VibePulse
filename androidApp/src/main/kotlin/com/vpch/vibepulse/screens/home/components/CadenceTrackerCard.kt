package com.vpch.vibepulse.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.CadenceZone
import com.vpch.vibepulse.presentation.home.RunStatus
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.VibePulseTheme
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText
import com.vpch.vibepulse.ui.mapper.toLabel
import com.vpch.vibepulse.ui.mapper.toSubtitleLabel

@Composable
fun CadenceTrackerCard(
    runStatus: RunStatus,
    currentCadence: Int,
    cadenceZone: CadenceZone = CadenceZone.fromSpm(currentCadence),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                TitleText(
                    text = currentCadence.toString(),
                    color = AppTheme.colors.textPrimary,
                    fontSize = 44.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                BodyText(
                    text = stringResource(R.string.cadence_unit_spm),
                    color = AppTheme.colors.indicator,
                    style = AppTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (runStatus == RunStatus.RUNNING) {
                        AppTheme.colors.buttonFilled.copy(alpha = 0.16f)
                    } else {
                        AppTheme.colors.buttonSecondary
                    }
                ) {
                    BodyText(
                        text = cadenceZone.toLabel(),
                        color = AppTheme.colors.textPrimary,
                        style = AppTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                BodyText(
                    text = runStatus.toSubtitleLabel(),
                    color = AppTheme.colors.textSecondary,
                    style = AppTheme.typography.labelSmall
                )
            }
        }
    }
}




@PreviewLightDark
@Composable
fun CadenceTrackerCardPreview() {
    VibePulseTheme {
        Surface(
            color = AppTheme.colors.background,
            modifier = Modifier.padding(16.dp)
        ) {
            CadenceTrackerCard(
                runStatus = RunStatus.RUNNING,
                currentCadence = 168
            )
        }
    }
}