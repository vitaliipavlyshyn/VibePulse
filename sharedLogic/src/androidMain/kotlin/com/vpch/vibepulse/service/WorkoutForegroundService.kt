package com.vpch.vibepulse.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.vpch.vibepulse.domain.repository.PlaybackRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WorkoutForegroundService : Service(), KoinComponent {

    private val playbackRepository: PlaybackRepository by inject()
    private val runTrackerNotifier: RunTrackerNotifier by inject()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OR_UPDATE -> {
                val cadence = intent.getIntExtra(EXTRA_CADENCE, 0)
                val elapsedSeconds = intent.getLongExtra(EXTRA_ELAPSED_SECONDS, 0L)
                val isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, false)

                val notification = buildNotification(cadence, elapsedSeconds, isPaused)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }

            ACTION_FINISH -> {
                runTrackerNotifier.notifyFinishClicked()
                playbackRepository.pauseImmediate()
                stopForegroundAndSelf()
            }

            ACTION_STOP -> {
                stopForegroundAndSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        playbackRepository.pauseImmediate()
        Thread.sleep(250L)
        playbackRepository.disconnectRemote()
        stopForegroundAndSelf()
        super.onTaskRemoved(rootIntent)
    }

    private fun stopForegroundAndSelf() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(
        cadence: Int,
        elapsedSeconds: Long,
        isPaused: Boolean
    ): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val finishIntent = Intent(this, WorkoutForegroundService::class.java).apply {
            action = ACTION_FINISH
        }
        val finishPendingIntent = PendingIntent.getService(
            this,
            1,
            finishIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusTitle = if (isPaused) "VibePulse · Пауза" else "VibePulse · Активне тренування"
        val formattedTime = formatTime(elapsedSeconds)
        val contentText = "Каденс: $cadence SPM · Час: $formattedTime"

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val finishAction = Notification.Action.Builder(
            null,
            "Фініш",
            finishPendingIntent
        ).build()

        return builder
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(statusTitle)
            .setContentText(contentText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(finishAction)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Тренування VibePulse",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Відображення поточного каденсу та керування активністю"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun formatTime(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

    companion object {
        private const val CHANNEL_ID = "vibepulse_workout_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_OR_UPDATE = "com.vpch.vibepulse.action.START_OR_UPDATE_WORKOUT"
        const val ACTION_FINISH = "com.vpch.vibepulse.action.FINISH_WORKOUT"
        const val ACTION_STOP = "com.vpch.vibepulse.action.STOP_WORKOUT"

        const val EXTRA_CADENCE = "extra_cadence"
        const val EXTRA_ELAPSED_SECONDS = "extra_elapsed_seconds"
        const val EXTRA_IS_PAUSED = "extra_is_paused"
    }
}