package com.vpch.vibepulse.sensor

import kotlin.concurrent.Volatile
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class StepCadenceDetector {
    private val gravity = FloatArray(3)
    private var hasGravity = false
    private var wasAboveThreshold = false
    private var smoothedSpm = 0.0

    private val stepMarks = ArrayDeque<TimeMark>()

    @Volatile
    private var lastStepMark: TimeMark? = null

    @Volatile
    private var cachedCadence: Int = 0

    @Volatile
    private var pendingReset: Boolean = false

    private var consecutiveSteps: Int = 0

    private val highThreshold = 1.45f
    private val lowThreshold = 0.65f
    private val minStepIntervalMs = 260L
    private val maxStepIntervalMs = 2000L
    private val requiredStepsToStart = 4
    private val windowDurationMs = 8000L
    private val holdDurationMs = 3500L
    private val inactivityTimeoutMs = 5500L

    fun registerHardwareStep() {
        if (pendingReset) resetInternal()
        registerPeak(TimeSource.Monotonic.markNow())
    }

    fun onAccelerometerSample(x: Float, y: Float, z: Float) {
        if (pendingReset) resetInternal()

        if (!hasGravity) {
            gravity[0] = x
            gravity[1] = y
            gravity[2] = z
            hasGravity = true
            return
        }

        val alpha = 0.88f
        gravity[0] = alpha * gravity[0] + (1f - alpha) * x
        gravity[1] = alpha * gravity[1] + (1f - alpha) * y
        gravity[2] = alpha * gravity[2] + (1f - alpha) * z

        val lx = x - gravity[0]
        val ly = y - gravity[1]
        val lz = z - gravity[2]
        val linearAcceleration = sqrt(lx * lx + ly * ly + lz * lz)

        if (!wasAboveThreshold && linearAcceleration > highThreshold) {
            wasAboveThreshold = true
            registerPeak(TimeSource.Monotonic.markNow())
        } else if (wasAboveThreshold && linearAcceleration < lowThreshold) {
            wasAboveThreshold = false
        }
    }

    private fun registerPeak(now: TimeMark) {
        val prev = lastStepMark
        val intervalMs = prev?.elapsedNow()?.inWholeMilliseconds ?: 0L

        if (prev != null && intervalMs < minStepIntervalMs) return

        if (prev == null || intervalMs > maxStepIntervalMs) {
            stepMarks.clear()
            stepMarks.addLast(now)
            lastStepMark = now
            consecutiveSteps = 1
            return
        }

        consecutiveSteps++
        lastStepMark = now
        stepMarks.addLast(now)

        while (stepMarks.isNotEmpty() && stepMarks.first().elapsedNow().inWholeMilliseconds > windowDurationMs) {
            stepMarks.removeFirst()
        }

        if (consecutiveSteps < requiredStepsToStart || stepMarks.size < 3) return

        val spanMs = stepMarks.first().elapsedNow().inWholeMilliseconds -
                stepMarks.last().elapsedNow().inWholeMilliseconds

        if (spanMs >= 1000L) {
            val rawSpm = ((stepMarks.size - 1) * 60_000.0 / spanMs).coerceIn(55.0, 210.0)
            smoothedSpm = if (smoothedSpm == 0.0) {
                rawSpm
            } else {
                smoothedSpm * 0.85 + rawSpm * 0.15
            }
            cachedCadence = smoothedSpm.roundToInt()
        }
    }

    fun getCadence(): Int {
        if (pendingReset) return 0
        val lastMark = lastStepMark ?: return 0
        val idleMs = lastMark.elapsedNow().inWholeMilliseconds

        if (idleMs > inactivityTimeoutMs) {
            return 0
        }
        if (idleMs <= holdDurationMs) {
            return cachedCadence
        }
        val progress = (idleMs - holdDurationMs).toDouble() / (inactivityTimeoutMs - holdDurationMs)
        return (cachedCadence * (1.0 - progress)).roundToInt().coerceAtLeast(0)
    }

    fun reset() {
        cachedCadence = 0
        lastStepMark = null
        pendingReset = true
    }

    private fun resetInternal() {
        pendingReset = false
        hasGravity = false
        wasAboveThreshold = false
        smoothedSpm = 0.0
        lastStepMark = null
        consecutiveSteps = 0
        cachedCadence = 0
        stepMarks.clear()
    }
}