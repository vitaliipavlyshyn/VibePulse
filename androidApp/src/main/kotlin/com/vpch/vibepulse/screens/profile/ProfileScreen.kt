@file:Suppress("UNCHECKED_CAST")

package com.vpch.vibepulse.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.UserTier
import com.vpch.vibepulse.presentation.profile.ProfileEffect
import com.vpch.vibepulse.presentation.profile.ProfileUiState
import com.vpch.vibepulse.presentation.profile.ProfileViewModel
import com.vpch.vibepulse.screens.profile.components.LogoutButton
import com.vpch.vibepulse.screens.profile.components.LogoutConfirmationDialog
import com.vpch.vibepulse.screens.profile.components.MusicPreferencesCard
import com.vpch.vibepulse.screens.profile.components.ProfileDefaults
import com.vpch.vibepulse.screens.profile.components.ProfileHeaderSection
import com.vpch.vibepulse.screens.profile.components.ThemeSelectionCard
import com.vpch.vibepulse.ui.AppThemeMode
import com.vpch.vibepulse.ui.ThemeRepositoryImpl
import com.vpch.vibepulse.ui.components.VibePulsePreview
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
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is ProfileEffect.NavigateToAuth) {
                onLogoutSuccess()
            }
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutDialog = false }
        )
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
        onLogoutClick = { showLogoutDialog = true }
    )
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    themeMode: AppThemeMode,
    bottomPadding: Dp = 0.dp,
    onSelectTheme: (AppThemeMode) -> Unit = {},
    onToggleGenre: (String) -> Unit = {},
    onWeatherToggle: (Boolean) -> Unit = {},
    onExcludeRemixesToggle: (Boolean) -> Unit = {},
    onExcludeExplicitToggle: (Boolean) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = ProfileDefaults.ScreenHorizontalPadding,
                end = ProfileDefaults.ScreenHorizontalPadding,
                top = ProfileDefaults.ScreenTopPadding,
                bottom = 16.dp + bottomPadding
            ),
        verticalArrangement = Arrangement.spacedBy(ProfileDefaults.SectionSpacing)
    ) {
        ProfileHeaderSection(
            displayName = uiState.user?.displayName,
            avatarUrl = uiState.user?.avatarUrl,
            tier = uiState.tier
        )

        MusicPreferencesCard(
            tier = uiState.tier,
            preferences = uiState.preferences,
            onToggleGenre = onToggleGenre,
            onWeatherToggle = onWeatherToggle,
            onExcludeRemixesToggle = onExcludeRemixesToggle,
            onExcludeExplicitToggle = onExcludeExplicitToggle
        )

        ThemeSelectionCard(
            selectedMode = themeMode,
            onSelectTheme = onSelectTheme
        )

        LogoutButton(onClick = onLogoutClick)
    }
}

@PreviewLightDark
@Composable
private fun ProfileScreenPreview() = VibePulsePreview(padding = 0.dp) {
    ProfileContent(
        uiState = ProfileUiState(
            user = User(spotifyId = "vitalii_123", displayName = "Віталій", hasPremium = true),
            tier = UserTier.PREMIUM,
            preferences = PlaylistPreferences(
                genres = setOf("pop", "electronic") as List<String>,
                weatherEnabled = true
            )
        ),
        themeMode = AppThemeMode.SYSTEM
    )
}