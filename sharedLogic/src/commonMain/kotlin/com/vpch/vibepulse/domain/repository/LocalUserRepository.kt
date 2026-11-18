package com.vpch.vibepulse.domain.repository


import com.vpch.vibepulse.domain.model.User

interface LocalUserRepository {
   suspend fun saveUser(user: User)
   suspend fun getUserById(spotifyId: String): User?
}