package com.vpch.vibepulse.domain.coordinator

import com.vpch.vibepulse.domain.model.Activity
import com.vpch.vibepulse.domain.model.PaceZone
import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.domain.service.PlaybackController
import com.vpch.vibepulse.domain.service.PlaylistGenerator
import com.vpch.vibepulse.domain.service.WeatherService
import com.vpch.vibepulse.sensor.CadenceMeter
import kotlin.math.abs
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class ActivityCoordinator(
    val cadenceMeter: CadenceMeter,
    val weatherService: WeatherService,
    val playlistGenerator: PlaylistGenerator,
    val playbackController: PlaybackController
) {
    var activity: Activity? = null
        private set

    var currentZone: PaceZone? = null
        private set

    private var lastAppliedCadence: Int = 0
    private val minuteSamples = mutableListOf<Int>()
    private val recentSamples = ArrayDeque<Int>()

    private var windowStartMark: TimeMark? = null
    private var lastZoneSwitchMark: TimeMark? = null
    private var lastSyncMark: TimeMark? = null
    private var isBusy = false

    private val evaluationIntervalSeconds = 60L
    private val syncIntervalSeconds = 10L
    private val fastReactionWindowSize = 8
    private val minSamplesForFastSwitch = 12
    private val fastSwitchCadenceDelta = 20
    private val switchCooldownSeconds = 20L

    fun startSession(user: User): Activity {
        currentZone = null
        lastAppliedCadence = 0
        minuteSamples.clear()
        recentSamples.clear()
        windowStartMark = TimeSource.Monotonic.markNow()
        lastZoneSwitchMark = null
        lastSyncMark = null
        isBusy = false
        playlistGenerator.resetHistory()
        playbackController.resetQueue()

        val initialActivity = Activity().let { base ->
            weatherService.lastSnapshot?.let { base.attachWeather(it) } ?: base
        }
        activity = initialActivity
        cadenceMeter.start()
        return initialActivity
    }

    suspend fun startInitialPlayback(weather: WeatherSnapshot?) {
        if (playbackController.isPlaying) return

        val initialTracks = playlistGenerator.pickTracks(
            zone = PaceZone.WALK,
            averageCadence = 110,
            weather = weather,
            preferences = null,
            count = 3
        )

        if (initialTracks.isNotEmpty()) {
            playbackController.playBatch(initialTracks).onSuccess {
                currentZone = PaceZone.WALK
                lastAppliedCadence = 110
                lastZoneSwitchMark = TimeSource.Monotonic.markNow()
            }
        } else {
            playbackController.resume()
        }
    }

    suspend fun onTick(
        preferences: PlaylistPreferences? = null
    ) {
        val currentSession = activity ?: return
        val currentCadence = cadenceMeter.getCadence()

        activity = currentSession.addSample(currentCadence)

        if (currentCadence >= 50) {
            minuteSamples.add(currentCadence)
            recentSamples.addLast(currentCadence)
            if (recentSamples.size > fastReactionWindowSize) {
                recentSamples.removeFirst()
            }
        } else if (recentSamples.isNotEmpty()) {
            recentSamples.removeFirst()
        }

        if (isBusy) return

        val recentAvg = if (recentSamples.size >= minSamplesForFastSwitch) {
            recentSamples.average().toInt()
        } else {
            0
        }
        val fastCandidateZone = if (recentAvg >= 50) {
            PaceZone.fromCadence(recentAvg, currentZone)
        } else {
            null
        }
        val cooldownPassed = (lastZoneSwitchMark?.elapsedNow()?.inWholeSeconds ?: 999L) >= switchCooldownSeconds
        val isSuddenShift = fastCandidateZone != null &&
                fastCandidateZone != currentZone &&
                abs(recentAvg - lastAppliedCadence) >= fastSwitchCadenceDelta &&
                cooldownPassed

        val elapsedWindow = windowStartMark?.elapsedNow()?.inWholeSeconds ?: 0L
        val isMinuteElapsed = elapsedWindow >= evaluationIntervalSeconds

        if (isSuddenShift || isMinuteElapsed) {
            isBusy = true
            try {
                val targetCadence = when {
                    isSuddenShift -> recentAvg
                    minuteSamples.isNotEmpty() -> minuteSamples.average().toInt()
                    else -> currentCadence
                }

                minuteSamples.clear()
                windowStartMark = TimeSource.Monotonic.markNow()

                if (targetCadence >= 50) {
                    val newZone = PaceZone.fromCadence(targetCadence, currentZone)
                    if (newZone != currentZone) {
                        val weather = fetchWeatherIfNeeded(preferences)
                        val batch = playlistGenerator.pickTracks(
                            zone = newZone,
                            averageCadence = targetCadence,
                            weather = weather,
                            preferences = preferences,
                            count = 3
                        )
                        if (batch.isNotEmpty()) {
                            playbackController.playBatch(batch).onSuccess {
                                currentZone = newZone
                                lastAppliedCadence = targetCadence
                                lastZoneSwitchMark = TimeSource.Monotonic.markNow()
                                recentSamples.clear()
                            }
                        }
                    }
                }
            } finally {
                isBusy = false
            }
            return
        }

        val syncInterval = if (playbackController.isRemoteConnected) 1L else 3L
        val elapsedSync = lastSyncMark?.elapsedNow()?.inWholeSeconds
        if (elapsedSync == null || elapsedSync >= syncInterval) {
            lastSyncMark = TimeSource.Monotonic.markNow()
            playbackController.syncPlaybackState()

            val activeZone = currentZone
            if (activeZone != null && playbackController.remainingTrackMs <= 25_000L) {
                val weather = fetchWeatherIfNeeded(preferences)
                val fallbackCadence = recentSamples.lastOrNull()
                    ?: minuteSamples.lastOrNull()
                    ?: activeZone.minBpm
                playbackController.enqueueNextIfNeeded {
                    playlistGenerator.pickTracks(
                        zone = activeZone,
                        averageCadence = fallbackCadence,
                        weather = weather,
                        preferences = preferences,
                        count = 1
                    ).firstOrNull()
                }
            }
        }
    }

    private suspend fun fetchWeatherIfNeeded(
        preferences: PlaylistPreferences?
    ): WeatherSnapshot? {
        if (preferences?.weatherEnabled == false) return weatherService.lastSnapshot
        val snapshot = weatherService.getWeather().getOrNull()
        if (snapshot != null && activity?.weather != snapshot) {
            activity = activity?.attachWeather(snapshot)
        }
        return snapshot
    }

    fun pauseSession() {
        cadenceMeter.stop()
    }

    fun resumeSession() {
        cadenceMeter.start()
    }

    fun endSession(): Activity? {
        cadenceMeter.stop()
        val finished = activity?.finish()
        activity = null
        currentZone = null
        lastAppliedCadence = 0
        minuteSamples.clear()
        recentSamples.clear()
        windowStartMark = null
        lastZoneSwitchMark = null
        lastSyncMark = null
        isBusy = false
        return finished
    }
}