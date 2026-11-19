package com.vpch.vibepulse.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat

class AndroidCadenceMeter(
    private val context: Context
) : CadenceMeter {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val detector = StepCadenceDetector()
    private var isListening = false

    override val currentCadence: Int
        get() = getCadence()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_STEP_DETECTOR -> {
                    if (event.values.firstOrNull() == 1.0f) {
                        detector.registerHardwareStep()
                    }
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    detector.onAccelerometerSample(
                        x = event.values[0],
                        y = event.values[1],
                        z = event.values[2]
                    )
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun start() {
        if (isListening) return
        detector.reset()

        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelSensor != null) {
            isListening = sensorManager.registerListener(
                listener,
                accelSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            return
        }

        val stepSensor = if (hasActivityRecognitionPermission()) {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        } else {
            null
        }
        if (stepSensor != null) {
            isListening = sensorManager.registerListener(
                listener,
                stepSensor,
                SensorManager.SENSOR_DELAY_FASTEST,
                0
            )
        }
    }

    override fun stop() {
        if (!isListening) return
        sensorManager.unregisterListener(listener)
        isListening = false
        detector.reset()
    }

    override fun getCadence(): Int = detector.getCadence()

    private fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}