package com.vpch.vibepulse.domain.repository

import com.vpch.vibepulse.domain.model.PlaylistPreferences
import kotlinx.coroutines.flow.StateFlow

interface PreferencesRepository {
    val preferences: StateFlow<PlaylistPreferences>
    fun update(transform: (PlaylistPreferences) -> PlaylistPreferences)
    fun reset()
}