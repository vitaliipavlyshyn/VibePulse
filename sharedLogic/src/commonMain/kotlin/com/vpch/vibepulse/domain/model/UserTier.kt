package com.vpch.vibepulse.domain.model

enum class UserTier(val hasPremiumAccess: Boolean) {
    FREE(hasPremiumAccess = false),
    TRIAL(hasPremiumAccess = true),
    PREMIUM(hasPremiumAccess = true)
}