package com.vpch.vibepulse.data.repository.remote

import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.repository.UserRepository
import com.vpch.vibepulse.network.SpotifyApiService
import com.vpch.vibepulse.utils.toDomain

class UserRepositoryImpl(
    private val spotifyApiService: SpotifyApiService
) : UserRepository {

    private var cachedUser: User? = null

    override suspend fun getCurrentUser(forceRefresh: Boolean): Result<User> {
        if (!forceRefresh) {
            cachedUser?.let { return Result.success(it) }
        }
        return spotifyApiService.getCurrentUser()
            .map { it.toDomain() }
            .onSuccess { cachedUser = it }
    }

    override fun clearCache() {
        cachedUser = null
    }
}