package com.vpch.vibepulse.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.R
import com.vpch.vibepulse.presentation.home.RunStatus
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.VibePulseTheme
import com.vpch.vibepulse.ui.components.button.VibePulseButton
import com.vpch.vibepulse.ui.components.text.BodyText

@Composable
internal fun RunControlsSection(
    runStatus: RunStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit
) {
    val isActive = runStatus != RunStatus.IDLE
    val isPaused = runStatus == RunStatus.PAUSED

    val finishHorizontalPadding by animateDpAsState(
        targetValue = if (isPaused) HomeDefaults.FinishPaddingPaused else HomeDefaults.FinishPaddingRunning,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "finishPadding"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VibePulseButton(
            onClick = {
                when (runStatus) {
                    RunStatus.IDLE -> onStart()
                    RunStatus.RUNNING -> onPause()
                    RunStatus.PAUSED -> onResume()
                }
            },
            shape = CircleShape,
            modifier = Modifier
                .weight(1f)
                .height(HomeDefaults.ControlButtonHeight)
        ) {
            AnimatedContent(
                targetState = runStatus,
                transitionSpec = {
                    (fadeIn(tween(HomeDefaults.ButtonFadeInDurationMs)) + expandHorizontally(expandFrom = Alignment.CenterHorizontally))
                        .togetherWith(fadeOut(tween(HomeDefaults.ButtonFadeOutDurationMs)))
                },
                label = "primaryRunButtonContent"
            ) { state ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (state != RunStatus.RUNNING) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.play_arrow),
                            contentDescription = null,
                            modifier = Modifier.size(HomeDefaults.ControlIconSize)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    BodyText(
                        text = when (state) {
                            RunStatus.IDLE -> stringResource(R.string.start_activity)
                            RunStatus.RUNNING -> stringResource(R.string.pause)
                            RunStatus.PAUSED -> stringResource(R.string.continue_)
                        },
                        color = AppTheme.colors.onButtonFilled,
                        style = AppTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(HomeDefaults.FinishEnterDurationMs)) + expandHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                expandFrom = Alignment.Start
            ),
            exit = fadeOut(tween(HomeDefaults.FinishExitDurationMs)) + shrinkHorizontally(
                animationSpec = tween(HomeDefaults.FinishEnterDurationMs),
                shrinkTowards = Alignment.Start
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(HomeDefaults.SectionSpacing))

                Surface(
                    onClick = onFinish,
                    shape = CircleShape,
                    color = AppTheme.colors.buttonPrimary,
                    border = BorderStroke(
                        width = HomeDefaults.CardBorderWidth,
                        color = AppTheme.colors.indicator.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.height(HomeDefaults.ControlButtonHeight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = finishHorizontalPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.flag),
                            contentDescription = null,
                            tint = AppTheme.colors.indicator,
                            modifier = Modifier.size(HomeDefaults.FinishFlagIconSize)
                        )
                        BodyText(
                            text = stringResource(R.string.finish),
                            color = AppTheme.colors.textPrimary,
                            style = AppTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun RunControlsSectionPreview() {
    VibePulseTheme {
        Surface(
            color = AppTheme.colors.background,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RunControlsSection(runStatus = RunStatus.IDLE, onStart = {}, onPause = {}, onResume = {}, onFinish = {})
                RunControlsSection(runStatus = RunStatus.RUNNING, onStart = {}, onPause = {}, onResume = {}, onFinish = {})
                RunControlsSection(runStatus = RunStatus.PAUSED, onStart = {}, onPause = {}, onResume = {}, onFinish = {})
            }
        }
    }
}