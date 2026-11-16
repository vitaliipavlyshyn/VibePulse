package com.vpch.vibepulse.domain.model

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class WeatherSnapshot(
    val area: String,
    val temperature: Double,
    val condition: String,
    val conditionMain: String = "",
    val iconUrl: String? = null,
    val fetchedAt: Instant = Clock.System.now()
) {
    fun isFresh(ttl: Duration = DEFAULT_CACHE_TTL): Boolean {
        val age = Clock.System.now() - fetchedAt
        return age < ttl
    }

    companion object {
        val DEFAULT_CACHE_TTL: Duration = 45.minutes
    }
}