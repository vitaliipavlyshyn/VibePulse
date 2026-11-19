package com.vpch.vibepulse.domain.model

data class ActivitySessionSnapshot(
    val activity: Activity? = null,
    val currentZone: PaceZone? = null,
    val currentCadence: Int = 0,
    val cadenceZone: CadenceZone = CadenceZone.IDLE,
    val isAutoPaused: Boolean = false
)