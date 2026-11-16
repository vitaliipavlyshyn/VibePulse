package com.vpch.vibepulse.presentation.profile

import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.User

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val isPremium: Boolean = false,
    val preferences: PlaylistPreferences = PlaylistPreferences()
)

sealed interface ProfileEffect {
    data object NavigateToAuth : ProfileEffect
}