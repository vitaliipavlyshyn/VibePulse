package com.vpch.vibepulse.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.presentation.profile.ProfileEffect
import com.vpch.vibepulse.presentation.profile.ProfileUiState
import com.vpch.vibepulse.presentation.profile.ProfileViewModel
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.AppThemeMode
import com.vpch.vibepulse.ui.ThemeRepositoryImpl
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    bottomPadding: Dp = 0.dp,
    onLogoutSuccess: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
    themeRepository: ThemeRepositoryImpl = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentThemeMode by themeRepository.themeMode.collectAsStateWithLifecycle(
        initialValue = AppThemeMode.SYSTEM
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is ProfileEffect.NavigateToAuth) {
                onLogoutSuccess()
            }
        }
    }

    ProfileContent(
        uiState = uiState,
        themeMode = currentThemeMode,
        bottomPadding = bottomPadding,
        onSelectTheme = { mode -> scope.launch { themeRepository.setThemeMode(mode) } },
        onToggleGenre = viewModel::toggleGenre,
        onWeatherToggle = viewModel::setWeatherEnabled,
        onExcludeRemixesToggle = viewModel::setExcludeRemixesAndCovers,
        onExcludeExplicitToggle = viewModel::setExcludeExplicit,
        onLogout = viewModel::logout
    )
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    themeMode: AppThemeMode,
    bottomPadding: Dp,
    onSelectTheme: (AppThemeMode) -> Unit,
    onToggleGenre: (String) -> Unit,
    onWeatherToggle: (Boolean) -> Unit,
    onExcludeRemixesToggle: (Boolean) -> Unit,
    onExcludeExplicitToggle: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp + bottomPadding
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val avatarUrl = uiState.user?.avatarUrl

            Surface(
                modifier = Modifier.size(84.dp),
                shape = CircleShape,
                color = AppTheme.colors.buttonSecondary,
                border = BorderStroke(2.dp, AppTheme.colors.indicator.copy(alpha = 0.7f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    TitleText(
                        text = uiState.user?.displayName?.firstOrNull()?.uppercase() ?: "V",
                        color = AppTheme.colors.textPrimary,
                        style = AppTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TitleText(
                text = uiState.user?.displayName ?: "Runner",
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = CircleShape,
                color = if (uiState.isPremium) {
                    AppTheme.colors.buttonFilled
                } else {
                    AppTheme.colors.buttonSecondary
                }
            ) {
                BodyText(
                    text = if (uiState.isPremium) "PREMIUM" else "FREE",
                    color = if (uiState.isPremium) {
                        AppTheme.colors.onButtonFilled
                    } else {
                        AppTheme.colors.textSecondary
                    },
                    style = AppTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = AppTheme.colors.buttonPrimary,
            border = BorderStroke(1.dp, AppTheme.colors.strokePrimary.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TitleText(
                        text = "Підбір музики",
                        color = AppTheme.colors.textPrimary,
                        style = AppTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!uiState.isPremium) {
                        Surface(
                            shape = CircleShape,
                            color = AppTheme.colors.buttonSecondary
                        ) {
                            BodyText(
                                text = "Лише Premium",
                                color = AppTheme.colors.indicator,
                                style = AppTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (!uiState.isPremium) {
                    BodyText(
                        text = "Вибір пріоритетних жанрів, адаптація під погоду та фільтрація реміксів і Explicit-треків доступні в Premium-версії.",
                        color = AppTheme.colors.textSecondary,
                        style = AppTheme.typography.bodyMedium
                    )
                } else {
                    BodyText(
                        text = "Пріоритетні жанри",
                        color = AppTheme.colors.textSecondary,
                        style = AppTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlaylistPreferences.AVAILABLE_GENRES.forEach { genre ->
                            val selected = uiState.preferences.genres.any {
                                it.equals(genre, ignoreCase = true)
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { onToggleGenre(genre) },
                                label = {
                                    BodyText(
                                        text = genre,
                                        color = if (selected) {
                                            AppTheme.colors.onButtonFilled
                                        } else {
                                            AppTheme.colors.textPrimary
                                        },
                                        style = AppTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppTheme.colors.buttonFilled,
                                    containerColor = AppTheme.colors.buttonSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = AppTheme.colors.strokePrimary.copy(alpha = 0.4f),
                                    selectedBorderColor = AppTheme.colors.buttonFilled
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = AppTheme.colors.strokePrimary.copy(alpha = 0.2f))

                    PreferenceToggleRow(
                        title = "Вплив погоди",
                        subtitle = "Підлаштовувати настрій треків під погодні умови",
                        checked = uiState.preferences.weatherEnabled,
                        onCheckedChange = onWeatherToggle
                    )

                    PreferenceToggleRow(
                        title = "Без реміксів і каверів",
                        subtitle = "Виключати кавер-версії, ремікси, live та sped-up треки",
                        checked = uiState.preferences.excludeRemixesAndCovers,
                        onCheckedChange = onExcludeRemixesToggle
                    )

                    PreferenceToggleRow(
                        title = "Без нецензурної лексики",
                        subtitle = "Блокувати треки з позначкою Explicit",
                        checked = uiState.preferences.excludeExplicit,
                        onCheckedChange = onExcludeExplicitToggle
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = AppTheme.colors.buttonPrimary,
            border = BorderStroke(1.dp, AppTheme.colors.strokePrimary.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TitleText(
                    text = "Тема оформлення",
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        AppThemeMode.SYSTEM to "Системна",
                        AppThemeMode.LIGHT to "Світла",
                        AppThemeMode.DARK to "Темна"
                    )
                    modes.forEach { (mode, label) ->
                        val isSelected = themeMode == mode
                        Surface(
                            onClick = { onSelectTheme(mode) },
                            shape = CircleShape,
                            color = if (isSelected) {
                                AppTheme.colors.buttonFilled
                            } else {
                                AppTheme.colors.buttonSecondary
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                BodyText(
                                    text = label,
                                    color = if (isSelected) {
                                        AppTheme.colors.onButtonFilled
                                    } else {
                                        AppTheme.colors.textPrimary
                                    },
                                    style = AppTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = AppTheme.colors.buttonPrimary,
            border = BorderStroke(1.dp, AppTheme.colors.textError.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BodyText(
                    text = "Вийти з акаунта Spotify",
                    color = AppTheme.colors.textError,
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
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