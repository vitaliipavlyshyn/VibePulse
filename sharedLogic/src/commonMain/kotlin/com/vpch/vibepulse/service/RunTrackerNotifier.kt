package com.vpch.vibepulse.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface RunTrackerNotifier {
    val finishRequests: SharedFlow<Unit>

    fun onRunStarted()
    fun onRunUpdated(cadence: Int, elapsedSeconds: Long, isPaused: Boolean)
    fun onRunStopped()
    fun notifyFinishClicked()
}

class NoOpRunTrackerNotifier : RunTrackerNotifier {
    private val _finishRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val finishRequests: SharedFlow<Unit> = _finishRequests.asSharedFlow()

    override fun onRunStarted() {}
    override fun onRunUpdated(cadence: Int, elapsedSeconds: Long, isPaused: Boolean) {}
    override fun onRunStopped() {}
    override fun notifyFinishClicked() {
        _finishRequests.tryEmit(Unit)
    }
}