package com.vpch.vibepulse.screens.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.LocationManager
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.presentation.home.HomeEffect
import com.vpch.vibepulse.presentation.home.HomeIntent
import com.vpch.vibepulse.presentation.home.HomeUiState
import com.vpch.vibepulse.presentation.home.HomeViewModel
import com.vpch.vibepulse.presentation.home.RunStatus
import com.vpch.vibepulse.screens.home.components.CadenceTrackerCard
import com.vpch.vibepulse.screens.home.components.HomeHeaderSection
import com.vpch.vibepulse.screens.home.components.RunControlsSection
import com.vpch.vibepulse.screens.home.components.SpotifyPlayerCard
import com.vpch.vibepulse.screens.home.components.WorkoutTimerCard
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.AppThemeMode
import com.vpch.vibepulse.ui.VibePulseTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onSessionExpired: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val successState = uiState as? HomeUiState.Success
        if (successState != null &&
            successState.weather == null &&
            successState.hasLocationPermission &&
            isSystemLocationEnabled(context)
        ) {
            viewModel.onIntent(HomeIntent.RefreshWeather)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.RequestLocationPermission -> {
                    permissionLauncher.launch(permissionsToRequest)
                }

                HomeEffect.OpenLocationSettings -> {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }

                HomeEffect.NavigateToAuth -> {
                    onSessionExpired()
                }

                is HomeEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeContent(
            uiState = uiState,
            onIntent = viewModel::onIntent,
            onRequestPermissions = { permissionLauncher.launch(permissionsToRequest) }
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
    modifier: Modifier = Modifier
) {
    when (uiState) {
        HomeUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator()
            }
        }

        is HomeUiState.Error -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onIntent(HomeIntent.LoadInitialData) }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.refresh),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Спробувати знову")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { onIntent(HomeIntent.LogoutClicked) }) {
                    Text("Увійти знову")
                }
            }
        }

        is HomeUiState.Success -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeHeaderSection(
                        user = uiState.user,
                        weather = uiState.weather,
                        isWeatherLoading = uiState.isWeatherLoading,
                        hasLocationPermission = uiState.hasLocationPermission,
                        isLocationEnabled = uiState.isLocationEnabled,
                        onRefreshWeather = { onIntent(HomeIntent.RefreshWeather) },
                        onRequestLocation = onRequestPermissions
                    )

                    CadenceTrackerCard(
                        runStatus = uiState.runStatus,
                        currentCadence = uiState.currentCadence,
                        cadenceZone = uiState.cadenceZone
                    )

                    SpotifyPlayerCard(
                        track = uiState.currentTrack,
                        isPlaying = uiState.isPlaying,
                        isLiked = uiState.isTrackLiked,
                        positionMs = uiState.trackPositionMs,
                        durationMs = uiState.trackDurationMs,
                        onToggleLike = { onIntent(HomeIntent.ToggleLikeClicked) },
                        onTogglePlayPause = { onIntent(HomeIntent.TogglePlayPauseClicked) },
                        onSkipNext = { onIntent(HomeIntent.SkipNextClicked) },
                        onSeekTo = { positionMs -> onIntent(HomeIntent.SeekTo(positionMs)) }
                    )

                    WorkoutTimerCard(
                        runStatus = uiState.runStatus,
                        elapsedSeconds = uiState.elapsedSeconds
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                RunControlsSection(
                    runStatus = uiState.runStatus,
                    onStart = { onIntent(HomeIntent.StartRunClicked) },
                    onPause = { onIntent(HomeIntent.PauseRunClicked) },
                    onResume = { onIntent(HomeIntent.ResumeRunClicked) },
                    onFinish = { onIntent(HomeIntent.FinishRunClicked) }
                )
            }
        }
    }
}


@Preview(showBackground = true, name = "Home - Running (Light)")
@Composable
fun HomeScreenRunningPreview() {
    VibePulseTheme(themeMode = AppThemeMode.LIGHT) {
        Surface(color = AppTheme.colors.background) {
            HomeContent(
                uiState = HomeUiState.Success(
                    user = User(
                        spotifyId = "vitalii_123",
                        displayName = "Віталій",
                        hasPremium = true
                    ),
                    weather = WeatherSnapshot(
                        temperature = 18.4,
                        condition = "Ясно",
                        area = "Львів"
                    ),
                    currentTrack = Track(
                        spotifyUri = "spotify:track:123",
                        title = "Blinding Lights",
                        artist = "The Weeknd",
                        genre = "pop",
                        tempo = 171,
                        durationMs = 200_000L
                    ),
                    isPlaying = true,
                    isTrackLiked = true,
                    runStatus = RunStatus.RUNNING,
                    currentCadence = 168,
                    elapsedSeconds = 845L,
                    trackPositionMs = 84_000L,
                    trackDurationMs = 200_000L,
                    hasLocationPermission = true,
                    isLocationEnabled = true
                ),
                onIntent = {},
                onRequestPermissions = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Home - Idle (Dark)"
)
@Composable
fun HomeScreenIdleDarkPreview() {
    VibePulseTheme(themeMode = AppThemeMode.DARK) {
        Surface(color = AppTheme.colors.background) {
            HomeContent(
                uiState = HomeUiState.Success(
                    user = User(
                        spotifyId = "vitalii_123",
                        displayName = "Віталій",
                        hasPremium = false
                    ),
                    runStatus = RunStatus.IDLE,
                    currentCadence = 0,
                    elapsedSeconds = 0L,
                    hasLocationPermission = false
                ),
                onIntent = {},
                onRequestPermissions = {}
            )
        }
    }
}

private fun isSystemLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return false
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

