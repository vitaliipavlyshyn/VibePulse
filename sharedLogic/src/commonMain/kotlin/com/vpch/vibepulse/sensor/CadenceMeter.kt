package com.vpch.vibepulse.sensor

import kotlin.concurrent.Volatile
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.TimeMark
import kotlin.time.TimeSource

interface CadenceMeter {
    val currentCadence: Int

    fun start()
    fun stop()
    fun getCadence(): Int
}

class StepCadenceDetector {
    private var gravity = 9.81f
    private var wasAboveThreshold = false
    private var smoothedSpm = 0.0
    private val stepMarks = ArrayDeque<TimeMark>()

    @Volatile
    private var lastStepMark: TimeMark? = null

    private val stepThreshold = 1.35f
    private val minStepIntervalMs = 300L
    private val windowDurationMs = 8000L
    private val inactivityTimeoutMs = 3500L
    private val minStepsToCalculate = 3

    fun onAccelerometerSample(x: Float, y: Float, z: Float) {
        val magnitude = sqrt(x * x + y * y + z * z)
        gravity = 0.92f * gravity + 0.08f * magnitude
        val linearAcceleration = magnitude - gravity
        val now = TimeSource.Monotonic.markNow()

        if (linearAcceleration > stepThreshold && !wasAboveThreshold) {
            wasAboveThreshold = true

            val previousMark = stepMarks.lastOrNull()
            if (previousMark != null && previousMark.elapsedNow().inWholeMilliseconds > inactivityTimeoutMs) {
                stepMarks.clear()
                smoothedSpm = 0.0
            }

            val currentLast = stepMarks.lastOrNull()
            if (currentLast == null || currentLast.elapsedNow().inWholeMilliseconds >= minStepIntervalMs) {
                stepMarks.addLast(now)
                lastStepMark = now

                while (stepMarks.isNotEmpty() && stepMarks.first().elapsedNow().inWholeMilliseconds > windowDurationMs) {
                    stepMarks.removeFirst()
                }

                if (stepMarks.size >= minStepsToCalculate) {
                    val spanMs = stepMarks.first().elapsedNow().inWholeMilliseconds -
                            stepMarks.last().elapsedNow().inWholeMilliseconds
                    if (spanMs > 0L) {
                        val rawSpm = ((stepMarks.size - 1) * 60_000.0 / spanMs).coerceIn(40.0, 210.0)
                        smoothedSpm = if (smoothedSpm == 0.0) rawSpm else smoothedSpm * 0.7 + rawSpm * 0.3
                    }
                }
            }
        } else if (linearAcceleration < stepThreshold * 0.4f) {
            wasAboveThreshold = false
        }
    }

    fun getCadence(): Int {
        val lastMark = lastStepMark ?: return 0
        if (lastMark.elapsedNow().inWholeMilliseconds > inactivityTimeoutMs) {
            return 0
        }
        return smoothedSpm.roundToInt()
    }

    fun reset() {
        gravity = 9.81f
        wasAboveThreshold = false
        smoothedSpm = 0.0
        lastStepMark = null
        stepMarks.clear()
    }
}