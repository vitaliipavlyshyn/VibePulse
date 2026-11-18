package com.vpch.vibepulse.data.repository.remote

import com.vpch.vibepulse.domain.repository.AuthRepository
import com.vpch.vibepulse.network.SpotifyAuthService
import com.vpch.vibepulse.storage.TokenStorage

class AuthRepositoryImpl(
    private val spotifyAuthService: SpotifyAuthService,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override fun hasSession(): Boolean = tokenStorage.hasSession()

    override fun clearSession() = tokenStorage.clearTokens()

    override fun buildAuthorizationUrl(): String =
        spotifyAuthService.buildAuthorizationUrl()

    override suspend fun exchangeCodeForToken(code: String): Result<Unit> =
        spotifyAuthService.exchangeCodeForToken(code).map { }
}