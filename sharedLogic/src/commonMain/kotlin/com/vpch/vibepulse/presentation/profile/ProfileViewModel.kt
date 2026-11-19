package com.vpch.vibepulse.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpch.vibepulse.domain.model.AppTierConfig
import com.vpch.vibepulse.domain.model.UserTier
import com.vpch.vibepulse.domain.repository.PreferencesRepository
import com.vpch.vibepulse.domain.repository.UserRepository
import com.vpch.vibepulse.domain.usecase.LogoutUseCase
import com.vpch.vibepulse.storage.SubscriptionManager
import com.vpch.vibepulse.storage.SubscriptionState
import com.vpch.vibepulse.storage.SubscriptionStatus
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
    private val subscriptionManager: SubscriptionManager,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        subscriptionManager.refresh()
        observeSubscriptionAndPreferences()
        onIntent(ProfileIntent.LoadProfile)
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.LoadProfile -> loadProfile()
            is ProfileIntent.ToggleGenre -> toggleGenre(intent.genre)
            is ProfileIntent.ToggleWeatherAdaptation -> setWeatherEnabled(intent.enabled)
            is ProfileIntent.ToggleExcludeRemixesAndCovers -> setExcludeRemixesAndCovers(intent.exclude)
            is ProfileIntent.ToggleExcludeExplicit -> setExcludeExplicit(intent.exclude)
            ProfileIntent.LogoutClicked -> logout()
        }
    }

    private fun observeSubscriptionAndPreferences() {
        viewModelScope.launch {
            subscriptionManager.state.collect { subState ->
                updateSuccessState { it.copy(tier = resolveUserTier(subState)) }
            }
        }

        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                updateSuccessState {
                    it.copy(preferencesState = it.preferencesState.copy(preferences = prefs))
                }
            }
        }
    }

    private fun resolveUserTier(subState: SubscriptionState): UserTier = when {
        tierConfig.isPremium -> UserTier.PREMIUM
        subState.status == SubscriptionStatus.ACTIVE && subState.isTrial -> UserTier.TRIAL
        subState.status == SubscriptionStatus.ACTIVE -> UserTier.PREMIUM
        else -> UserTier.FREE
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            userRepository.getCurrentUser()
                .onSuccess { user ->
                    _uiState.value = ProfileUiState.Success(
                        user = user,
                        tier = resolveUserTier(subscriptionManager.state.value),
                        preferencesState = PreferencesUiState(
                            preferences = preferencesRepository.preferences.value
                        )
                    )
                }
                .onFailure { error ->
                    val msg = error.message?.takeIf { it.isNotBlank() }
                        ?.let { ProfileMessage.Custom(it) }
                        ?: ProfileMessage.ProfileLoadError
                    _uiState.value = ProfileUiState.Error(message = msg)
                }
        }
    }

    private fun toggleGenre(genre: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        if (!current.tier.hasPremiumAccess) return
        preferencesRepository.update { it.toggleGenre(genre) }
    }

    private fun setWeatherEnabled(enabled: Boolean) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        if (!current.tier.hasPremiumAccess) return
        preferencesRepository.update { it.copy(weatherEnabled = enabled) }
    }

    private fun setExcludeRemixesAndCovers(exclude: Boolean) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        if (!current.tier.hasPremiumAccess) return
        preferencesRepository.update { it.copy(excludeRemixesAndCovers = exclude) }
    }

    private fun setExcludeExplicit(exclude: Boolean) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        if (!current.tier.hasPremiumAccess) return
        preferencesRepository.update { it.copy(excludeExplicit = exclude) }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _effects.send(ProfileEffect.NavigateToAuth)
        }
    }

    private inline fun updateSuccessState(
        crossinline transform: (ProfileUiState.Success) -> ProfileUiState.Success
    ) {
        _uiState.update { current ->
            if (current is ProfileUiState.Success) transform(current) else current
        }
    }
}