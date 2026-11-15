package com.vpch.vibepulse.domain.model

data class User(
    val spotifyId: String,
    val displayName: String,
    val hasPremium: Boolean,
    val avatarUrl: String? = null
) {
    fun signIn(): Boolean = spotifyId.isNotBlank()
    fun signOut() {
        TODO()
    }
}