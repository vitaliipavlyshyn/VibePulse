package com.vpch.vibepulse.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vpch.vibepulse.presentation.auth.AuthEffect
import com.vpch.vibepulse.presentation.auth.AuthIntent
import com.vpch.vibepulse.presentation.auth.AuthUiState
import com.vpch.vibepulse.presentation.auth.AuthViewModel
import com.vpch.vibepulse.ui.VibePulseTheme
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
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is AuthUiState.Loading -> {
                CircularProgressIndicator()
            }

            is AuthUiState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error: ${uiState.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onIntent(AuthIntent.ConnectSpotifyClicked) }
                    ) {
                        Text("Try Again")
                    }
                }
            }

            is AuthUiState.Idle,
            is AuthUiState.Success -> {
                Button(
                    onClick = { onIntent(AuthIntent.ConnectSpotifyClicked) }
                ) {
                    Text("Connect Spotify")
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

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview(
    @PreviewParameter(AuthStatePreviewProvider::class) state: AuthUiState
) {
    VibePulseTheme {
        AuthScreenContent(
            uiState = state,
            onIntent = {}
        )
    }
}