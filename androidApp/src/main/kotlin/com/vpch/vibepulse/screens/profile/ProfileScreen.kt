package com.vpch.vibepulse.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.UserTier
import com.vpch.vibepulse.presentation.profile.PreferencesUiState
import com.vpch.vibepulse.presentation.profile.ProfileEffect
import com.vpch.vibepulse.presentation.profile.ProfileIntent
import com.vpch.vibepulse.presentation.profile.ProfileMessage
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
import com.vpch.vibepulse.ui.components.ErrorStateView
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
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToAuth -> onLogoutSuccess()
                is ProfileEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message.resolve(context))
                }
            }
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.onIntent(ProfileIntent.LogoutClicked)
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ProfileContent(
            uiState = uiState,
            themeMode = currentThemeMode,
            bottomPadding = bottomPadding,
            onSelectTheme = { mode -> scope.launch { themeRepository.setThemeMode(mode) } },
            onIntent = viewModel::onIntent,
            onLogoutClick = { showLogoutDialog = true }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    themeMode: AppThemeMode,
    bottomPadding: Dp = 0.dp,
    onSelectTheme: (AppThemeMode) -> Unit = {},
    onIntent: (ProfileIntent) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    when (uiState) {
        ProfileUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }

        is ProfileUiState.Error -> {
            val context = LocalContext.current
            ErrorStateView(
                message = uiState.message.resolve(context),
                onRetry = { onIntent(ProfileIntent.LoadProfile) },
                secondaryActionLabel = stringResource(R.string.home_relogin),
                onSecondaryAction = { onIntent(ProfileIntent.LogoutClicked) }
            )
        }

        is ProfileUiState.Success -> {
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
                    preferences = uiState.preferencesState.preferences,
                    onToggleGenre = { genre -> onIntent(ProfileIntent.ToggleGenre(genre)) },
                    onWeatherToggle = { enabled -> onIntent(ProfileIntent.ToggleWeatherAdaptation(enabled)) },
                    onExcludeRemixesToggle = { exclude ->
                        onIntent(ProfileIntent.ToggleExcludeRemixesAndCovers(exclude))
                    },
                    onExcludeExplicitToggle = { exclude ->
                        onIntent(ProfileIntent.ToggleExcludeExplicit(exclude))
                    }
                )

                ThemeSelectionCard(
                    selectedMode = themeMode,
                    onSelectTheme = onSelectTheme
                )

                LogoutButton(onClick = onLogoutClick)
            }
        }
    }
}

private fun ProfileMessage.resolve(context: android.content.Context): String = when (this) {
    ProfileMessage.ProfileLoadError -> context.getString(R.string.home_error_profile)
    ProfileMessage.LogoutError -> context.getString(R.string.home_error_player_control)
    is ProfileMessage.Custom -> text
}

@PreviewLightDark
@Composable
private fun ProfileScreenPreview() = VibePulsePreview(padding = 0.dp) {
    ProfileContent(
        uiState = ProfileUiState.Success(
            user = User(spotifyId = "vitalii_123", displayName = "Віталій", hasPremium = true),
            tier = UserTier.PREMIUM,
            preferencesState = PreferencesUiState(
                preferences = PlaylistPreferences(
                    genres = listOf("pop", "electronic"),
                    weatherEnabled = true
                )
            )
        ),
        themeMode = AppThemeMode.SYSTEM
    )
}