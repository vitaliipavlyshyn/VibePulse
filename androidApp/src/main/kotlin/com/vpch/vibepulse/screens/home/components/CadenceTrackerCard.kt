package com.vpch.vibepulse.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.CadenceZone
import com.vpch.vibepulse.presentation.home.RunStatus
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.VibePulseCard
import com.vpch.vibepulse.ui.components.VibePulsePillBadge
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText
import com.vpch.vibepulse.ui.mapper.toLabel
import com.vpch.vibepulse.ui.mapper.toSubtitleLabel

@Composable
internal fun CadenceTrackerCard(
    modifier: Modifier = Modifier,
    runStatus: RunStatus,
    currentCadence: Int,
    cadenceZone: CadenceZone = CadenceZone.fromSpm(currentCadence)
) {
    VibePulseCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                TitleText(
                    text = currentCadence.toString(),
                    color = AppTheme.colors.textPrimary,
                    fontSize = HomeDefaults.CadenceValueFontSize,
                    lineHeight = HomeDefaults.CadenceValueLineHeight,
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
                VibePulsePillBadge(
                    text = cadenceZone.toLabel(),
                    containerColor = if (runStatus == RunStatus.RUNNING) {
                        AppTheme.colors.buttonFilled.copy(alpha = 0.16f)
                    } else {
                        AppTheme.colors.buttonSecondary
                    },
                    borderColor = null,
                    textStyle = AppTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
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
private fun CadenceTrackerCardPreview() = VibePulsePreview {
    CadenceTrackerCard(runStatus = RunStatus.RUNNING, currentCadence = 168)
}