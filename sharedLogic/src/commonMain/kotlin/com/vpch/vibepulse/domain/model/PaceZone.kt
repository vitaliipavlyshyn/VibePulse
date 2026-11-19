package com.vpch.vibepulse.domain.model

enum class PaceZone(
    val minBpm: Int,
    val maxBpm: Int,
    val searchTag: String
) {
    WALK(0, 124, "power walk chill"),
    JOG(125, 149, "jogging upbeat"),
    RUN(150, 174, "running workout"),
    SPRINT(175, 300, "fast running high energy");
    companion object {
        private const val HYSTERESIS = 4

        fun fromCadence(cadence: Int, currentZone: PaceZone?): PaceZone {
            if (currentZone != null) {
                val stayMin = (currentZone.minBpm - HYSTERESIS).coerceAtLeast(0)
                val stayMax = currentZone.maxBpm + HYSTERESIS
                if (cadence in stayMin..stayMax) return currentZone
            }
            return entries.firstOrNull { cadence in it.minBpm..it.maxBpm } ?: JOG
        }
    }
}