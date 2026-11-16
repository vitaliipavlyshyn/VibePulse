package com.vpch.vibepulse

import android.Manifest
import android.Manifest.permission
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
fun RequestInitialPermissions(
    onPermissionsResult: (locationGranted: Boolean) -> Unit
) {
    val permissions = remember {
        buildList {
            add(permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val locationGranted = result[permission.ACCESS_COARSE_LOCATION] == true
        onPermissionsResult(locationGranted)
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }
}