package com.vpch.vibepulse.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidLocationProvider(
    private val context: Context
) : LocationProvider {

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

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }

            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(
                        Result.success(Coordinates(location.latitude, location.longitude))
                    )
                } else {
                    fusedClient.lastLocation
                        .addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                continuation.resume(
                                    Result.success(Coordinates(lastLoc.latitude, lastLoc.longitude))
                                )
                            } else {
                                continuation.resume(
                                    Result.failure(IllegalStateException("Location unavailable"))
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            continuation.resume(Result.failure(e))
                        }
                }
            }.addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
        }
    }
}