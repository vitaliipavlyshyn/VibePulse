package com.vpch.vibepulse.screens.profile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.R
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.AppThemeMode
import com.vpch.vibepulse.ui.components.VibePulseCard
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText

@Composable
internal fun ThemeSelectionCard(
    selectedMode: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit
) {
    VibePulseCard(
        shape = RoundedCornerShape(ProfileDefaults.CardCornerRadius),
        borderColor = AppTheme.colors.strokePrimary.copy(alpha = 0.4f),
        borderWidth = 1.dp,
        elevation = 0.dp,
        contentPadding = PaddingValues(ProfileDefaults.CardPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TitleText(
                text = stringResource(R.string.profile_theme_title),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val modes = listOf(
                    AppThemeMode.SYSTEM to stringResource(R.string.profile_theme_system),
                    AppThemeMode.LIGHT to stringResource(R.string.profile_theme_light),
                    AppThemeMode.DARK to stringResource(R.string.profile_theme_dark)
                )
                modes.forEachIndexed { index, (mode, label) ->
                    val isSelected = selectedMode == mode
                    val targetStartRadius = if (isSelected || index == 0) ProfileDefaults.GroupOuterRadius else ProfileDefaults.GroupInnerRadius
                    val targetEndRadius = if (isSelected || index == modes.lastIndex) ProfileDefaults.GroupOuterRadius else ProfileDefaults.GroupInnerRadius

                    val startRadius by animateDpAsState(
                        targetValue = targetStartRadius,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                        label = "themeStartRadius"
                    )
                    val endRadius by animateDpAsState(
                        targetValue = targetEndRadius,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                        label = "themeEndRadius"
                    )
                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) AppTheme.colors.buttonFilled else AppTheme.colors.buttonSecondary,
                        label = "themeContainerColor"
                    )

                    Surface(
                        onClick = { onSelectTheme(mode) },
                        shape = RoundedCornerShape(
                            topStart = startRadius,
                            bottomStart = startRadius,
                            topEnd = endRadius,
                            bottomEnd = endRadius
                        ),
                        color = containerColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            BodyText(
                                text = label,
                                color = if (isSelected) AppTheme.colors.onButtonFilled else AppTheme.colors.textPrimary,
                                style = AppTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ThemeSelectionCardPreview() = VibePulsePreview {
    ThemeSelectionCard(selectedMode = AppThemeMode.SYSTEM, onSelectTheme = {})
}