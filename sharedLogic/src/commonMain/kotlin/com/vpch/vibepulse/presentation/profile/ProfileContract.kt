package com.vpch.vibepulse.presentation.profile

import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.UserTier

data class PreferencesUiState(
    val preferences: PlaylistPreferences = PlaylistPreferences(),
    val isSaving: Boolean = false
)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Success(
        val user: User? = null,
        val tier: UserTier = UserTier.FREE,
        val preferencesState: PreferencesUiState = PreferencesUiState()
    ) : ProfileUiState

    data class Error(val message: ProfileMessage) : ProfileUiState
}

sealed interface ProfileIntent {
    data object LoadProfile : ProfileIntent
    data class ToggleGenre(val genre: String) : ProfileIntent
    data class ToggleWeatherAdaptation(val enabled: Boolean) : ProfileIntent
    data class ToggleExcludeRemixesAndCovers(val exclude: Boolean) : ProfileIntent
    data class ToggleExcludeExplicit(val exclude: Boolean) : ProfileIntent
    data object LogoutClicked : ProfileIntent
}

sealed interface ProfileEffect {
    data object NavigateToAuth : ProfileEffect
    data class ShowMessage(val message: ProfileMessage) : ProfileEffect
}