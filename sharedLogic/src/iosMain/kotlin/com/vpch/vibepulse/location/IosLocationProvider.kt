package com.vpch.vibepulse.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

class IosLocationProvider : LocationProvider {

    override fun hasLocationPermission(): Boolean {
        val status = CLLocationManager().authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                status == kCLAuthorizationStatusAuthorizedAlways
    }

    override fun isLocationEnabled(): Boolean {
        return CLLocationManager.locationServicesEnabled()
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getCurrentLocation(): Result<Coordinates> = withContext(Dispatchers.Main) {
        if (!CLLocationManager.locationServicesEnabled()) {
            return@withContext Result.failure(IllegalStateException("Location services are disabled"))
        }

        suspendCancellableCoroutine { continuation ->
            val manager = CLLocationManager().apply {
                desiredAccuracy = kCLLocationAccuracyHundredMeters
            }
            var isResumed = false
            var activeDelegate: NSObject? = null

            fun finish(result: Result<Coordinates>) {
                if (!isResumed && continuation.isActive) {
                    isResumed = true
                    manager.delegate = null
                    activeDelegate = null
                    continuation.resume(result)
                }
            }

            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                    val location = didUpdateLocations.lastOrNull() as? CLLocation
                    if (location != null) {
                        val coords = location.coordinate.useContents {
                            Coordinates(latitude = latitude, longitude = longitude)
                        }
                        finish(Result.success(coords))
                    } else {
                        finish(Result.failure(IllegalStateException("Location is null")))
                    }
                }

                override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                    finish(Result.failure(Exception(didFailWithError.localizedDescription)))
                }

                override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                    when (manager.authorizationStatus) {
                        kCLAuthorizationStatusAuthorizedWhenInUse,
                        kCLAuthorizationStatusAuthorizedAlways -> manager.requestLocation()

                        kCLAuthorizationStatusDenied,
                        kCLAuthorizationStatusRestricted ->
                            finish(Result.failure(IllegalStateException("Location permission denied")))

                        else -> Unit
                    }
                }
            }

            activeDelegate = delegate
            manager.delegate = delegate

            continuation.invokeOnCancellation {
                manager.stopUpdatingLocation()
                manager.delegate = null
                activeDelegate = null
            }

            when (manager.authorizationStatus) {
                kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
                kCLAuthorizationStatusAuthorizedWhenInUse,
                kCLAuthorizationStatusAuthorizedAlways -> manager.requestLocation()
                else -> finish(Result.failure(IllegalStateException("Location permission denied")))
            }
        }
    }
}