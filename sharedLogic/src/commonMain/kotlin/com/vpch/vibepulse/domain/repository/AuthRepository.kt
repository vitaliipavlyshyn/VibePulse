package com.vpch.vibepulse.domain.repository

interface AuthRepository {
    fun hasSession(): Boolean
    fun clearSession()
    fun buildAuthorizationUrl(): String
    suspend fun exchangeCodeForToken(code: String): Result<Unit>
}