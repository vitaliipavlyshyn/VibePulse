package com.vpch.vibepulse.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

class AndroidLocationProvider(
    private val context: Context
) : LocationProvider {

    private companion object {
        const val CURRENT_LOCATION_TIMEOUT_MS = 10_000L
    }

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<Coordinates> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        if (!isLocationEnabled()) {
            return Result.failure(LocationDisabledException())
        }

        val freshLocation = withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT_MS.milliseconds) {
            requestFreshLocation()
        }

        if (freshLocation != null) {
            return Result.success(Coordinates(freshLocation.latitude, freshLocation.longitude))
        }

        val fallbackLocation = requestLastLocation()
        return if (fallbackLocation != null) {
            Result.success(Coordinates(fallbackLocation.latitude, fallbackLocation.longitude))
        } else {
            Result.failure(IllegalStateException("Location timeout or unavailable"))
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }

            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }.addOnFailureListener {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun requestLastLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            fusedClient.lastLocation
                .addOnSuccessListener { lastLoc ->
                    if (continuation.isActive) {
                        continuation.resume(lastLoc)
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
        }
}