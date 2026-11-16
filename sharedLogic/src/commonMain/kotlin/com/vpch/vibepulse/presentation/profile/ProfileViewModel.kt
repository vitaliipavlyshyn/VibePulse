package com.vpch.vibepulse.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpch.vibepulse.domain.model.AppTierConfig
import com.vpch.vibepulse.domain.repository.PreferencesRepository
import com.vpch.vibepulse.domain.repository.UserRepository
import com.vpch.vibepulse.domain.usecase.LogoutUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val preferencesRepository: PreferencesRepository,
    private val tierConfig: AppTierConfig,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            isPremium = tierConfig.isPremium,
            preferences = preferencesRepository.preferences.value
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadProfile()
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userRepository.getCurrentUser()
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            isPremium = tierConfig.isPremium
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun toggleGenre(genre: String) {
        if (!tierConfig.isPremium) return
        preferencesRepository.update { it.toggleGenre(genre) }
    }

    fun setWeatherEnabled(enabled: Boolean) {
        if (!tierConfig.isPremium) return
        preferencesRepository.update { it.copy(weatherEnabled = enabled) }
    }

    fun setExcludeRemixesAndCovers(exclude: Boolean) {
        if (!tierConfig.isPremium) return
        preferencesRepository.update { it.copy(excludeRemixesAndCovers = exclude) }
    }

    fun setExcludeExplicit(exclude: Boolean) {
        if (!tierConfig.isPremium) return
        preferencesRepository.update { it.copy(excludeExplicit = exclude) }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _effects.send(ProfileEffect.NavigateToAuth)
        }
    }
}