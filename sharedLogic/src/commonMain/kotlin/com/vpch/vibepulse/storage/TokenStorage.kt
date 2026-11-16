package com.vpch.vibepulse.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlin.time.Clock

interface TokenStorage {
    fun saveTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Int)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun isTokenExpired(): Boolean
    fun hasSession(): Boolean
    fun clearTokens()
}
