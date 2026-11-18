package com.vpch.vibepulse.sensor

import kotlin.concurrent.Volatile
import kotlin.math.abs
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
