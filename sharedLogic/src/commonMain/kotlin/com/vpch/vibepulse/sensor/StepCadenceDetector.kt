package com.vpch.vibepulse.sensor

import kotlin.concurrent.Volatile
import kotlin.math.abs
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
    private var lastPeakMark: TimeMark? = null
    private var lastIntervalMs: Long = 0L
    private var consecutiveRhythmicSteps: Int = 0

    private val stepThreshold = 1.85f
    private val minStepIntervalMs = 260L
    private val maxStepIntervalMs = 1500L
    private val requiredConsecutiveSteps = 4
    private val windowDurationMs = 8000L
    private val inactivityTimeoutMs = 3000L

    fun registerHardwareStep() {
        val now = TimeSource.Monotonic.markNow()
        val prev = lastStepMark
        if (prev != null && prev.elapsedNow().inWholeMilliseconds < minStepIntervalMs) {
            return
        }

        if (prev != null && prev.elapsedNow().inWholeMilliseconds > inactivityTimeoutMs) {
            stepMarks.clear()
            smoothedSpm = 0.0
        }

        consecutiveRhythmicSteps = requiredConsecutiveSteps
        lastStepMark = now
        lastPeakMark = now
        stepMarks.addLast(now)
        trimOldSteps()
        recalculateSpm()
    }

    fun onAccelerometerSample(x: Float, y: Float, z: Float) {
        if (!hasGravity) {
            gravity[0] = x
            gravity[1] = y
            gravity[2] = z
            hasGravity = true
            return
        }

        val alpha = 0.8f
        gravity[0] = alpha * gravity[0] + (1f - alpha) * x
        gravity[1] = alpha * gravity[1] + (1f - alpha) * y
        gravity[2] = alpha * gravity[2] + (1f - alpha) * z

        val lx = x - gravity[0]
        val ly = y - gravity[1]
        val lz = z - gravity[2]
        val linearAcceleration = sqrt(lx * lx + ly * ly + lz * lz)
        val now = TimeSource.Monotonic.markNow()

        if (linearAcceleration > stepThreshold && !wasAboveThreshold) {
            wasAboveThreshold = true

            val prevPeak = lastPeakMark
            val interval = prevPeak?.elapsedNow()?.inWholeMilliseconds ?: 0L

            if (prevPeak != null && interval < minStepIntervalMs) return

            if (prevPeak == null || interval > maxStepIntervalMs) {
                lastPeakMark = now
                lastIntervalMs = 0L
                consecutiveRhythmicSteps = 1
                stepMarks.clear()
                return
            }

            val isConsistentRhythm = lastIntervalMs == 0L ||
                    abs(interval - lastIntervalMs) <= (lastIntervalMs * 0.35f)

            lastPeakMark = now
            lastIntervalMs = interval

            if (isConsistentRhythm) {
                consecutiveRhythmicSteps++
                lastStepMark = now
                stepMarks.addLast(now)
                trimOldSteps()

                if (consecutiveRhythmicSteps >= requiredConsecutiveSteps) {
                    recalculateSpm()
                }
            } else {
                consecutiveRhythmicSteps = 1
                lastIntervalMs = 0L
                stepMarks.clear()
            }
        } else if (linearAcceleration < stepThreshold * 0.4f) {
            wasAboveThreshold = false
        }
    }

    fun getCadence(): Int {
        val lastMark = lastStepMark ?: return 0
        if (lastMark.elapsedNow().inWholeMilliseconds > inactivityTimeoutMs) {
            reset()
            return 0
        }
        if (consecutiveRhythmicSteps < requiredConsecutiveSteps) {
            return 0
        }
        return smoothedSpm.roundToInt()
    }

    fun reset() {
        hasGravity = false
        wasAboveThreshold = false
        smoothedSpm = 0.0
        lastStepMark = null
        lastPeakMark = null
        lastIntervalMs = 0L
        consecutiveRhythmicSteps = 0
        stepMarks.clear()
    }

    private fun trimOldSteps() {
        while (stepMarks.isNotEmpty() && stepMarks.first().elapsedNow().inWholeMilliseconds > windowDurationMs) {
            stepMarks.removeFirst()
        }
    }

    private fun recalculateSpm() {
        if (stepMarks.size < 2) return
        val spanMs = stepMarks.first().elapsedNow().inWholeMilliseconds -
                stepMarks.last().elapsedNow().inWholeMilliseconds
        if (spanMs > 0L) {
            val rawSpm = ((stepMarks.size - 1) * 60_000.0 / spanMs).coerceIn(40.0, 220.0)
            smoothedSpm = if (smoothedSpm == 0.0) rawSpm else smoothedSpm * 0.65 + rawSpm * 0.35
        }
    }
}