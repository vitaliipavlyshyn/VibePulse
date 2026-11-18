package com.vpch.vibepulse.data.repository.local

import com.vpch.vibepulse.data.local.dao.ActivityDao
import com.vpch.vibepulse.data.local.mapper.toDomain
import com.vpch.vibepulse.data.local.mapper.toEntity
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.repository.LocalUserRepository

class LocalUserRepositoryImpl(
    private val activityDao: ActivityDao
) : LocalUserRepository {

    override suspend fun saveUser(user: User) {
        activityDao.upsertUser(user.toEntity())
    }

    override suspend fun getUserById(spotifyId: String): User? {
        return activityDao.getUserById(spotifyId)?.toDomain()
    }
}