package com.vpch.vibepulse.domain.model

data class User(
    val spotifyId: String,
    val displayName: String,
    val hasPremium: Boolean,
    val avatarUrl: String? = null
) {
    constructor(
        id: String,
        displayName: String,
        email: String? = null,
        avatarUrl: String? = null,
        hasPremium: Boolean = false
    ) : this(
        spotifyId = id,
        displayName = displayName,
        hasPremium = hasPremium,
        avatarUrl = avatarUrl
    )

    fun signIn(): Boolean = spotifyId.isNotBlank()
    fun signOut() {
        TODO()
    }
}