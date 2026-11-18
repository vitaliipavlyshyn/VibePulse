package com.vpch.vibepulse.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.presentation.home.HomeEffect
import com.vpch.vibepulse.presentation.home.HomeIntent
import com.vpch.vibepulse.presentation.home.HomeUiState
import com.vpch.vibepulse.presentation.home.HomeViewModel
import com.vpch.vibepulse.presentation.home.PlayerUiState
import com.vpch.vibepulse.presentation.home.RunStatus
import com.vpch.vibepulse.presentation.home.RunUiState
import com.vpch.vibepulse.presentation.home.WeatherUiState
import com.vpch.vibepulse.screens.home.components.CadenceTrackerCard
import com.vpch.vibepulse.screens.home.components.HomeDefaults
import com.vpch.vibepulse.screens.home.components.HomeHeaderSection
import com.vpch.vibepulse.screens.home.components.RunControlsSection
import com.vpch.vibepulse.screens.home.components.SpotifyPlayerCard
import com.vpch.vibepulse.screens.home.components.WorkoutTimerCard
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.VibePulseTheme
import com.vpch.vibepulse.ui.components.ErrorStateView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onSessionExpired: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val locationGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onIntent(HomeIntent.LocationPermissionUpdated(locationGranted))
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.RequestLocationPermission -> permissionLauncher.launch(permissionsToRequest)
                HomeEffect.OpenLocationSettings -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                HomeEffect.NavigateToAuth -> onSessionExpired()
                is HomeEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeContent(
            uiState = uiState,
            onIntent = viewModel::onIntent,
            onRequestPermissions = { permissionLauncher.launch(permissionsToRequest) },
            onNavigateToProfile = onNavigateToProfile
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
fun HomeContent(
    uiState: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit = {}
) {
    when (uiState) {
        HomeUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }

        is HomeUiState.Error -> {
            ErrorStateView(
                message = uiState.message,
                onRetry = { onIntent(HomeIntent.LoadInitialData) },
                secondaryActionLabel = stringResource(R.string.home_relogin),
                onSecondaryAction = { onIntent(HomeIntent.LogoutClicked) },
                modifier = modifier
            )
        }

        is HomeUiState.Success -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = HomeDefaults.ScreenHorizontalPadding,
                        vertical = HomeDefaults.ScreenVerticalPadding
                    )
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(HomeDefaults.SectionSpacing)
                ) {
                    HomeHeaderSection(
                        user = uiState.user,
                        weather = uiState.weather.snapshot,
                        isWeatherLoading = uiState.weather.isLoading,
                        hasLocationPermission = uiState.weather.hasPermission,
                        isLocationEnabled = uiState.weather.isLocationEnabled,
                        onRefreshWeather = { onIntent(HomeIntent.RefreshWeather) },
                        onRequestLocation = onRequestPermissions,
                        onProfileClick = onNavigateToProfile
                    )

                    CadenceTrackerCard(
                        runStatus = uiState.run.status,
                        currentCadence = uiState.run.currentCadence,
                        cadenceZone = uiState.run.cadenceZone
                    )

                    SpotifyPlayerCard(
                        track = uiState.player.currentTrack,
                        isPlaying = uiState.player.isPlaying,
                        isLiked = uiState.player.isLiked,
                        positionMs = uiState.player.positionMs,
                        durationMs = uiState.player.durationMs,
                        onToggleLike = { onIntent(HomeIntent.ToggleLikeClicked) },
                        onTogglePlayPause = { onIntent(HomeIntent.TogglePlayPauseClicked) },
                        onSkipNext = { onIntent(HomeIntent.SkipNextClicked) },
                        onSeekTo = { positionMs -> onIntent(HomeIntent.SeekTo(positionMs)) }
                    )

                    WorkoutTimerCard(
                        runStatus = uiState.run.status,
                        elapsedSeconds = uiState.run.elapsedSeconds
                    )
                }

                Spacer(modifier = Modifier.height(HomeDefaults.SectionSpacing))

                RunControlsSection(
                    runStatus = uiState.run.status,
                    onStart = { onIntent(HomeIntent.StartRunClicked) },
                    onPause = { onIntent(HomeIntent.PauseRunClicked) },
                    onResume = { onIntent(HomeIntent.ResumeRunClicked) },
                    onFinish = { onIntent(HomeIntent.FinishRunClicked) }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    VibePulseTheme {
        Surface(color = AppTheme.colors.background) {
            HomeContent(
                uiState = HomeUiState.Success(
                    user = User(spotifyId = "vitalii_123", displayName = "Віталій", hasPremium = true),
                    weather = WeatherUiState(
                        snapshot = WeatherSnapshot(temperature = 18.4, condition = "Ясно", area = "Львів"),
                        hasPermission = true,
                        isLocationEnabled = true
                    ),
                    run = RunUiState(status = RunStatus.RUNNING, currentCadence = 168, elapsedSeconds = 845L),
                    player = PlayerUiState(
                        currentTrack = Track(
                            spotifyUri = "spotify:track:123",
                            title = "Blinding Lights",
                            artist = "The Weeknd",
                            genre = "pop",
                            tempo = 171,
                            durationMs = 200_000L
                        ),
                        isPlaying = true,
                        isLiked = true,
                        positionMs = 84_000L,
                        durationMs = 200_000L
                    )
                ),
                onIntent = {},
                onRequestPermissions = {}
            )
        }
    }
}