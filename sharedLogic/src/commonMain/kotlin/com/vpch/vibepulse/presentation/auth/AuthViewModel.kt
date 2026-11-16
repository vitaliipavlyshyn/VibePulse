package com.vpch.vibepulse.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpch.vibepulse.domain.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects: Flow<AuthEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.ConnectSpotifyClicked -> handleConnectSpotify()
            is AuthIntent.AuthCodeReceived -> handleAuthCodeReceived(code = intent.code)
            is AuthIntent.AuthErrorReceived -> {
                _uiState.value = AuthUiState.Error(intent.error)
            }
        }
    }

    private fun handleConnectSpotify() {
        _uiState.value = AuthUiState.Loading
        val url = authRepository.buildAuthorizationUrl()
        viewModelScope.launch {
            _effects.send(AuthEffect.OpenBrowser(url))
        }
    }

    private fun handleAuthCodeReceived(code: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.exchangeCodeForToken(code)
                .onSuccess {
                    _uiState.value = AuthUiState.Success
                    _effects.send(AuthEffect.NavigateToHome)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to authenticate")
                }
        }
    }
}