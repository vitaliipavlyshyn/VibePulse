package com.vpch.vibepulse.domain.model

import kotlin.time.Clock
import kotlin.time.Instant

data class CadenceSample(
    val timestamp: Instant = Clock.System.now(),
    val stepsPerMinute: Int,
    val trackUri: String? = null
)