package com.vpch.vibepulse.domain.model

import kotlin.time.Clock
import kotlin.time.Instant

data class Activity(
    val startTime: Instant = Clock.System.now(),
    val endTime: Instant? = null,
    val totalSteps: Int = 0,
    val avgCadence: Double = 0.0,
    val maxCadence: Double = 0.0,
    val weather: WeatherSnapshot? = null,
    val samples: List<CadenceSample> = emptyList()
) {
    fun isInProgress(): Boolean = endTime == null

    fun attachWeather(weather: WeatherSnapshot): Activity =
        copy(weather = weather)

    fun addSample(cadence: Int): Activity {
        val updatedSamples = samples + CadenceSample(stepsPerMinute = cadence)
        return copy(samples = updatedSamples).calculateStatistics()
    }

    fun calculateStatistics(): Activity {
        if (samples.isEmpty()) return this
        val avg = samples.map { it.stepsPerMinute }.average()
        val max = samples.maxOf { it.stepsPerMinute }.toDouble()
        val steps = samples.sumOf { (it.stepsPerMinute / 60.0).toInt() }
        return copy(
            totalSteps = steps,
            avgCadence = avg,
            maxCadence = max
        )
    }

    fun finish(): Activity =
        calculateStatistics().copy(endTime = Clock.System.now())
}