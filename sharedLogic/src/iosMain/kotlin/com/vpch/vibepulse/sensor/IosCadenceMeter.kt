package com.vpch.vibepulse.sensor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

class IosCadenceMeter : CadenceMeter {

    private val motionManager = CMMotionManager()
    private val detector = StepCadenceDetector()

    override val currentCadence: Int
        get() = detector.getCadence()

    @OptIn(ExperimentalForeignApi::class)
    override fun start() {
        if (!motionManager.accelerometerAvailable) return
        detector.reset()

        motionManager.accelerometerUpdateInterval = 0.02
        val queue = NSOperationQueue.mainQueue

        motionManager.startAccelerometerUpdatesToQueue(queue) { data, _ ->
            data?.acceleration?.useContents {
                val gToMs2 = 9.80665f
                detector.onAccelerometerSample(
                    x = x.toFloat() * gToMs2,
                    y = y.toFloat() * gToMs2,
                    z = z.toFloat() * gToMs2
                )
            }
        }
    }

    override fun stop() {
        if (motionManager.accelerometerActive) {
            motionManager.stopAccelerometerUpdates()
        }
        detector.reset()
    }

    override fun getCadence(): Int = detector.getCadence()
}