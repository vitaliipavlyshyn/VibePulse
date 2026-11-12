package com.vpch.vibepulse.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface TokenStorage {
    fun saveTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Int)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun isTokenExpired(): Boolean
    fun hasSession(): Boolean
    fun clearTokens()
}

@OptIn(ExperimentalTime::class)
class SecureTokenStorage(
    private val settings: Settings
) : TokenStorage {

    override fun saveTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Int) {
        val expiresAtMillis = Clock.System.now().toEpochMilliseconds() + (expiresInSeconds * 1000L)

        settings[KEY_ACCESS_TOKEN] = accessToken
        settings[KEY_EXPIRES_AT] = expiresAtMillis

        if (refreshToken != null) {
            settings[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    override fun getAccessToken(): String? =
        settings.getStringOrNull(KEY_ACCESS_TOKEN)

    override fun getRefreshToken(): String? =
        settings.getStringOrNull(KEY_REFRESH_TOKEN)

    override fun isTokenExpired(): Boolean {
        val expiresAt = settings.getLongOrNull(KEY_EXPIRES_AT) ?: return true
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return currentTime >= (expiresAt - 60_000L)
    }

    override fun hasSession(): Boolean =
        !getAccessToken().isNullOrBlank() || !getRefreshToken().isNullOrBlank()

    override fun clearTokens() {
        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
        settings.remove(KEY_EXPIRES_AT)
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "spotify_access_token"
        const val KEY_REFRESH_TOKEN = "spotify_refresh_token"
        const val KEY_EXPIRES_AT = "spotify_expires_at"
    }
}