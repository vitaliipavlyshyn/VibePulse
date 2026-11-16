package com.vpch.vibepulse.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlaylistPreferencesStore(
    private val settings: Settings
) : PreferencesRepository {
    private val _preferences = MutableStateFlow(loadFromSettings())
    override val preferences: StateFlow<PlaylistPreferences> = _preferences.asStateFlow()

    override fun update(transform: (PlaylistPreferences) -> PlaylistPreferences) {
        _preferences.update { current ->
            val updated = transform(current)
            saveToSettings(updated)
            updated
        }
    }

    override fun reset() {
        val defaults = PlaylistPreferences()
        saveToSettings(defaults)
        _preferences.value = defaults
    }

    private fun loadFromSettings(): PlaylistPreferences {
        val genresRaw = settings.getStringOrNull(KEY_GENRES).orEmpty()
        val genres = if (genresRaw.isBlank()) {
            emptyList()
        } else {
            genresRaw.split(",").filter { it.isNotBlank() }
        }

        return PlaylistPreferences(
            weatherEnabled = settings.getBoolean(KEY_WEATHER_ENABLED, defaultValue = true),
            genres = genres,
            excludeRemixesAndCovers = settings.getBoolean(KEY_EXCLUDE_REMIXES, defaultValue = false),
            excludeExplicit = settings.getBoolean(KEY_EXCLUDE_EXPLICIT, defaultValue = false)
        )
    }

    private fun saveToSettings(prefs: PlaylistPreferences) {
        settings[KEY_WEATHER_ENABLED] = prefs.weatherEnabled
        settings[KEY_GENRES] = prefs.genres.joinToString(",")
        settings[KEY_EXCLUDE_REMIXES] = prefs.excludeRemixesAndCovers
        settings[KEY_EXCLUDE_EXPLICIT] = prefs.excludeExplicit
    }

    private companion object {
        const val KEY_WEATHER_ENABLED = "pref_weather_enabled"
        const val KEY_GENRES = "pref_genres"
        const val KEY_EXCLUDE_REMIXES = "pref_exclude_remixes"
        const val KEY_EXCLUDE_EXPLICIT = "pref_exclude_explicit"
    }
}