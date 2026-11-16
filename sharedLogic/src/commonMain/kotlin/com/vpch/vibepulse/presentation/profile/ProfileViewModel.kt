package com.vpch.vibepulse.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpch.vibepulse.domain.coordinator.ActivityCoordinator
import com.vpch.vibepulse.domain.model.AppTierConfig
import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.network.SpotifyApiService
import com.vpch.vibepulse.service.RunTrackerNotifier
import com.vpch.vibepulse.storage.PlaylistPreferencesStore
import com.vpch.vibepulse.storage.TokenStorage
import com.vpch.vibepulse.utils.toDomain
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val isPremium: Boolean = false,
    val preferences: PlaylistPreferences = PlaylistPreferences()
)

sealed interface ProfileEffect {
    data object NavigateToAuth : ProfileEffect
}

class ProfileViewModel(
    private val spotifyApiService: SpotifyApiService,
    private val preferencesStore: PlaylistPreferencesStore,
    private val tierConfig: AppTierConfig,
    private val tokenStorage: TokenStorage,
    private val activityCoordinator: ActivityCoordinator,
    private val runTrackerNotifier: RunTrackerNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            isPremium = tierConfig.isPremium,
            preferences = preferencesStore.preferences.value
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadProfile()
        viewModelScope.launch {
            preferencesStore.preferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            spotifyApiService.getCurrentUser()
                .onSuccess { dto ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = dto.toDomain(),
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
        preferencesStore.update { it.toggleGenre(genre) }
    }

    fun setWeatherEnabled(enabled: Boolean) {
        if (!tierConfig.isPremium) return
        preferencesStore.update { it.copy(weatherEnabled = enabled) }
    }

    fun setExcludeRemixesAndCovers(exclude: Boolean) {
        if (!tierConfig.isPremium) return
        preferencesStore.update { it.copy(excludeRemixesAndCovers = exclude) }
    }

    fun setExcludeExplicit(exclude: Boolean) {
        if (!tierConfig.isPremium) return
        preferencesStore.update { it.copy(excludeExplicit = exclude) }
    }

    fun logout() {
        viewModelScope.launch {
            runTrackerNotifier.onRunStopped()
            activityCoordinator.playbackController.pauseImmediate()
            activityCoordinator.playbackController.pause()
            activityCoordinator.endSession()
            tokenStorage.clearTokens()
            _effects.send(ProfileEffect.NavigateToAuth)
        }
    }
}