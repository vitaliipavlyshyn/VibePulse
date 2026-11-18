@file:Suppress("UNCHECKED_CAST")

package com.vpch.vibepulse.screens.profile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.UserTier
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.VibePulseCard
import com.vpch.vibepulse.ui.components.VibePulsePillBadge
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText

@Composable
internal fun MusicPreferencesCard(
    tier: UserTier,
    preferences: PlaylistPreferences,
    onToggleGenre: (String) -> Unit,
    onWeatherToggle: (Boolean) -> Unit,
    onExcludeRemixesToggle: (Boolean) -> Unit,
    onExcludeExplicitToggle: (Boolean) -> Unit
) {
    val hasAccess = tier.hasPremiumAccess

    VibePulseCard(
        shape = RoundedCornerShape(ProfileDefaults.CardCornerRadius),
        borderColor = AppTheme.colors.strokePrimary.copy(alpha = 0.4f),
        borderWidth = 1.dp,
        elevation = 0.dp,
        contentPadding = PaddingValues(vertical = ProfileDefaults.CardPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ProfileDefaults.CardPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TitleText(
                    text = stringResource(R.string.profile_music_selection_title),
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (!hasAccess) {
                    VibePulsePillBadge(
                        text = stringResource(R.string.profile_premium_only_badge),
                        contentColor = AppTheme.colors.indicator,
                        borderColor = null,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    val selectedCount = preferences.genres.size
                    BodyText(
                        text = if (selectedCount == 0) {
                            stringResource(R.string.profile_genres_auto_tempo)
                        } else {
                            stringResource(R.string.profile_genres_selected_count, selectedCount)
                        },
                        color = if (selectedCount > 0) AppTheme.colors.indicator else AppTheme.colors.textSecondary,
                        style = AppTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!hasAccess) {
                BodyText(
                    text = stringResource(R.string.profile_premium_upsell_description),
                    color = AppTheme.colors.textSecondary,
                    style = AppTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = ProfileDefaults.CardPadding)
                )
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = ProfileDefaults.CardPadding),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items = PlaylistPreferences.AVAILABLE_GENRES, key = { it }) { genre ->
                        CompactGenreChip(
                            label = genre,
                            selected = preferences.genres.any { it.equals(genre, ignoreCase = true) },
                            onClick = { onToggleGenre(genre) }
                        )
                    }
                }

                HorizontalDivider(
                    color = AppTheme.colors.strokePrimary.copy(alpha = 0.2f),
                    modifier = Modifier.padding(horizontal = ProfileDefaults.CardPadding)
                )

                val toggles = listOf(
                    Triple(
                        stringResource(R.string.profile_pref_weather_title),
                        stringResource(R.string.profile_pref_weather_subtitle),
                        preferences.weatherEnabled to onWeatherToggle
                    ),
                    Triple(
                        stringResource(R.string.profile_pref_no_remixes_title),
                        stringResource(R.string.profile_pref_no_remixes_subtitle),
                        preferences.excludeRemixesAndCovers to onExcludeRemixesToggle
                    ),
                    Triple(
                        stringResource(R.string.profile_pref_no_explicit_title),
                        stringResource(R.string.profile_pref_no_explicit_subtitle),
                        preferences.excludeExplicit to onExcludeExplicitToggle
                    )
                )

                Column(
                    modifier = Modifier.padding(horizontal = ProfileDefaults.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    toggles.forEachIndexed { index, (title, subtitle, stateAndCallback) ->
                        PreferenceToggleRow(
                            title = title,
                            subtitle = subtitle,
                            checked = stateAndCallback.first,
                            onCheckedChange = stateAndCallback.second
                        )
                        if (index < toggles.lastIndex) {
                            HorizontalDivider(color = AppTheme.colors.strokePrimary.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactGenreChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (selected) ProfileDefaults.SelectedChipRadius else ProfileDefaults.UnselectedChipRadius,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "genreChipCorner"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) AppTheme.colors.buttonFilled else AppTheme.colors.buttonSecondary,
        label = "genreChipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) AppTheme.colors.buttonFilled else AppTheme.colors.strokePrimary.copy(alpha = 0.25f),
        label = "genreChipBorder"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        BodyText(
            text = label,
            color = if (selected) AppTheme.colors.onButtonFilled else AppTheme.colors.textPrimary,
            style = AppTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            BodyText(
                text = title,
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            BodyText(
                text = subtitle,
                color = AppTheme.colors.textSecondary,
                style = AppTheme.typography.bodySmall
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppTheme.colors.onButtonFilled,
                checkedTrackColor = AppTheme.colors.buttonFilled
            )
        )
    }
}

@PreviewLightDark
@Composable
private fun MusicPreferencesCardPreview() = VibePulsePreview {
    MusicPreferencesCard(
        tier = UserTier.PREMIUM,
        preferences = PlaylistPreferences(genres = setOf("pop", "rock") as List<String>, weatherEnabled = true),
        onToggleGenre = {},
        onWeatherToggle = {},
        onExcludeRemixesToggle = {},
        onExcludeExplicitToggle = {}
    )
}