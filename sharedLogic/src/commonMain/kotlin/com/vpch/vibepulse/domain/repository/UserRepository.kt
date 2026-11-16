package com.vpch.vibepulse.domain.repository

import com.vpch.vibepulse.domain.model.User

interface UserRepository {
    suspend fun getCurrentUser(forceRefresh: Boolean = false): Result<User>
    fun clearCache()
}