package com.vpch.vibepulse.domain.model

import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class WeatherSnapshot(
    val area: String,
    val temperature: Double,
    val condition: String,
    val fetchedAt: Instant = Clock.System.now()
) {
    fun isFresh(cacheTtlMinutes: Int = 45): Boolean {
        val age = Clock.System.now() - fetchedAt
        return age < cacheTtlMinutes.minutes
    }
}
