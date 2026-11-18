package com.vpch.vibepulse.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vpch.vibepulse.R
import com.vpch.vibepulse.presentation.auth.AuthEffect
import com.vpch.vibepulse.presentation.auth.AuthIntent
import com.vpch.vibepulse.presentation.auth.AuthUiState
import com.vpch.vibepulse.presentation.auth.AuthViewModel
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.VibePulseTheme
import com.vpch.vibepulse.ui.components.AppErrorDialog
import com.vpch.vibepulse.ui.components.button.VibePulseButton
import com.vpch.vibepulse.ui.components.text.BodyText
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AuthEffect.OpenBrowser -> uriHandler.openUri(effect.url)
                is AuthEffect.NavigateToHome -> onLoginSuccess()
            }
        }
    }

    AuthScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
private fun AuthScreenContent(
    uiState: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showErrorDialog by remember(uiState) {
        mutableStateOf(uiState is AuthUiState.Error)
    }

    if (uiState is AuthUiState.Error && showErrorDialog) {
        AppErrorDialog(
            title = "Помилка входу",
            message = uiState.message,
            onDismiss = { showErrorDialog = false },
            onConfirm = { onIntent(AuthIntent.ConnectSpotifyClicked) }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is AuthUiState.Loading -> {
                CircularWavyProgressIndicator()
            }

            is AuthUiState.Idle,
            is AuthUiState.Success,
            is AuthUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    BodyText(
                        text = stringResource(R.string.vibe_pulse),
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = AppTheme.colors.title,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    VibePulseButton(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .height(48.dp),
                        onClick = { onIntent(AuthIntent.ConnectSpotifyClicked) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.spotify_logo),
                                contentDescription = "Spotify Logo",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.connect_spotify)
                            )
                        }
                    }
                }
            }
        }
    }
}


private class AuthStatePreviewProvider : PreviewParameterProvider<AuthUiState> {
    override val values = sequenceOf(
        AuthUiState.Idle,
        AuthUiState.Loading,
        AuthUiState.Error("Invalid authorization code")
    )
}

@PreviewLightDark
@Composable
private fun AuthScreenPreview(
    @PreviewParameter(AuthStatePreviewProvider::class) state: AuthUiState
) {
    VibePulseTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AuthScreenContent(
                uiState = state,
                onIntent = {}
            )
        }
    }
}