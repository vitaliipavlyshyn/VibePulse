package com.vpch.vibepulse.domain.model

import kotlin.time.Clock
import kotlin.time.Instant

data class Activity(
    val id: Long = 0L,
    val startTime: Instant = Clock.System.now(),
    val endTime: Instant? = null,
    val activeDurationSeconds: Long = 0L,
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
        val nonZeroSamples = samples.filter { it.stepsPerMinute > 0 }
        val avg = if (nonZeroSamples.isNotEmpty()) {
            nonZeroSamples.map { it.stepsPerMinute }.average()
        } else {
            0.0
        }
        val max = samples.maxOf { it.stepsPerMinute }.toDouble()
        val steps = kotlin.math.round(samples.sumOf { it.stepsPerMinute / 60.0 }).toInt()
        return copy(
            totalSteps = steps,
            avgCadence = avg,
            maxCadence = max
        )
    }

    fun finish(): Activity =
        calculateStatistics().copy(endTime = Clock.System.now())
}