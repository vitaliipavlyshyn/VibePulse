package com.vpch.vibepulse.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.StateFlow

class AndroidCadenceMeter(
    context: Context
) : CadenceMeter, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val detector = StepCadenceDetector()

    override val currentCadence: Int
        get() = detector.getCadence()

    override fun start() {
        detector.reset()
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun stop() {
        sensorManager.unregisterListener(this)
        detector.reset()
    }

    override fun getCadence(): Int = detector.getCadence()

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            detector.onAccelerometerSample(
                x = event.values[0],
                y = event.values[1],
                z = event.values[2]
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}