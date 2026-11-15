package com.vpch.vibepulse.location

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

class LocationDisabledException(
    message: String = "Увімкніть геолокацію (GPS) на телефоні"
) : Exception(message)

interface LocationProvider {
    fun hasLocationPermission(): Boolean
    fun isLocationEnabled(): Boolean
    suspend fun getCurrentLocation(): Result<Coordinates>
}