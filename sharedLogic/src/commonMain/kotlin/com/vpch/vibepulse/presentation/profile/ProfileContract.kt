package com.vpch.vibepulse.presentation.profile

import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.UserTier

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val tier: UserTier = UserTier.FREE,
    val preferences: PlaylistPreferences = PlaylistPreferences()
)

sealed interface ProfileEffect {
    data object NavigateToAuth : ProfileEffect
}