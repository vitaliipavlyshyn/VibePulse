package com.vpch.vibepulse.service

import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AndroidRunTrackerNotifier(
    context: Context
) : RunTrackerNotifier {

    private val appContext = context.applicationContext
    private var isServiceRunning = false

    private val _finishRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val finishRequests: SharedFlow<Unit> = _finishRequests.asSharedFlow()

    override fun onRunStarted() {
        isServiceRunning = true
        val intent = Intent(appContext, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_START_OR_UPDATE
            putExtra(WorkoutForegroundService.EXTRA_CADENCE, 0)
            putExtra(WorkoutForegroundService.EXTRA_ELAPSED_SECONDS, 0L)
            putExtra(WorkoutForegroundService.EXTRA_IS_PAUSED, false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
    }

    override fun onRunUpdated(cadence: Int, elapsedSeconds: Long, isPaused: Boolean) {
        if (!isServiceRunning) return
        val intent = Intent(appContext, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_START_OR_UPDATE
            putExtra(WorkoutForegroundService.EXTRA_CADENCE, cadence)
            putExtra(WorkoutForegroundService.EXTRA_ELAPSED_SECONDS, elapsedSeconds)
            putExtra(WorkoutForegroundService.EXTRA_IS_PAUSED, isPaused)
        }
        appContext.startService(intent)
    }

    override fun onRunStopped() {
        if (!isServiceRunning) return
        isServiceRunning = false
        val intent = Intent(appContext, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_STOP
        }
        appContext.startService(intent)
    }

    override fun notifyFinishClicked() {
        isServiceRunning = false
        _finishRequests.tryEmit(Unit)
    }
}