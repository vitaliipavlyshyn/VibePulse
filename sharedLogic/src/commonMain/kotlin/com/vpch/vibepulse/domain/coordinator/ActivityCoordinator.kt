package com.vpch.vibepulse.domain.coordinator

import com.vpch.vibepulse.domain.model.Activity
import com.vpch.vibepulse.domain.model.CadenceZone
import com.vpch.vibepulse.domain.model.PaceZone
import com.vpch.vibepulse.domain.model.PlaylistPreferences
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.domain.repository.PlaybackRepository
import com.vpch.vibepulse.domain.repository.WeatherRepository
import com.vpch.vibepulse.domain.usecase.GenerateWorkoutPlaylistUseCase
import com.vpch.vibepulse.sensor.CadenceMeter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.time.TimeMark
import kotlin.time.TimeSource

data class WorkoutSessionSnapshot(
    val activity: Activity? = null,
    val currentZone: PaceZone? = null,
    val currentCadence: Int = 0,
    val cadenceZone: CadenceZone = CadenceZone.IDLE
)

class ActivityCoordinator(
    private val cadenceMeter: CadenceMeter,
    private val weatherRepository: WeatherRepository,
    private val generatePlaylist: GenerateWorkoutPlaylistUseCase,
    private val playbackRepository: PlaybackRepository
) {
    private val _sessionState = MutableStateFlow(WorkoutSessionSnapshot())
    val sessionState: StateFlow<WorkoutSessionSnapshot> = _sessionState.asStateFlow()

    private var lastAppliedCadence: Int = 0
    private val minuteSamples = mutableListOf<Int>()
    private val recentSamples = ArrayDeque<Int>()

    private var windowStartMark: TimeMark? = null
    private var lastZoneSwitchMark: TimeMark? = null
    private var lastSyncMark: TimeMark? = null
    private var isBusy = false

    private val evaluationIntervalSeconds = 60L
    private val fastReactionWindowSize = 8
    private val minSamplesForFastSwitch = 12
    private val fastSwitchCadenceDelta = 20
    private val switchCooldownSeconds = 20L

    fun startSession(user: User): Activity {
        lastAppliedCadence = 0
        minuteSamples.clear()
        recentSamples.clear()
        windowStartMark = TimeSource.Monotonic.markNow()
        lastZoneSwitchMark = null
        lastSyncMark = null
        isBusy = false
        generatePlaylist.resetHistory()
        playbackRepository.resetQueue()

        val initialActivity = Activity().let { base ->
            weatherRepository.currentWeather.value?.let { base.attachWeather(it) } ?: base
        }
        _sessionState.value = WorkoutSessionSnapshot(activity = initialActivity)
        cadenceMeter.start()
        return initialActivity
    }

    suspend fun startInitialPlayback(
        weather: WeatherSnapshot?,
        preferences: PlaylistPreferences? = null
    ) {
        if (playbackRepository.state.value.isPlaying) return

        val initialTracks = generatePlaylist(
            zone = PaceZone.WALK,
            averageCadence = 110,
            weather = weather,
            preferences = preferences,
            count = 3
        )

        if (initialTracks.isNotEmpty()) {
            playbackRepository.playBatch(initialTracks).onSuccess {
                _sessionState.update { it.copy(currentZone = PaceZone.WALK) }
                lastAppliedCadence = 110
                lastZoneSwitchMark = TimeSource.Monotonic.markNow()
            }
        } else {
            playbackRepository.resume()
        }
    }

    suspend fun onTick(preferences: PlaylistPreferences? = null) {
        val currentActivity = _sessionState.value.activity ?: return
        val currentCadence = cadenceMeter.getCadence()
        val updatedActivity = currentActivity.addSample(currentCadence)

        _sessionState.update {
            it.copy(
                activity = updatedActivity,
                currentCadence = currentCadence,
                cadenceZone = CadenceZone.fromSpm(currentCadence)
            )
        }

        if (currentCadence >= 50) {
            minuteSamples.add(currentCadence)
            recentSamples.addLast(currentCadence)
            if (recentSamples.size > fastReactionWindowSize) recentSamples.removeFirst()
        } else if (recentSamples.isNotEmpty()) {
            recentSamples.removeFirst()
        }

        if (isBusy) return

        val currentZone = _sessionState.value.currentZone
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
                        val batch = generatePlaylist(
                            zone = newZone,
                            averageCadence = targetCadence,
                            weather = weather,
                            preferences = preferences,
                            count = 3
                        )
                        if (batch.isNotEmpty()) {
                            playbackRepository.playBatch(batch).onSuccess {
                                _sessionState.update { it.copy(currentZone = newZone) }
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

        val playbackState = playbackRepository.state.value
        val syncInterval = if (playbackState.isRemoteConnected) 1L else 3L
        val elapsedSync = lastSyncMark?.elapsedNow()?.inWholeSeconds
        if (elapsedSync == null || elapsedSync >= syncInterval) {
            lastSyncMark = TimeSource.Monotonic.markNow()
            playbackRepository.syncState()

            val activeZone = _sessionState.value.currentZone
            if (activeZone != null && playbackRepository.state.value.remainingMs <= 25_000L) {
                val weather = fetchWeatherIfNeeded(preferences)
                val fallbackCadence = recentSamples.lastOrNull()
                    ?: minuteSamples.lastOrNull()
                    ?: activeZone.minBpm
                playbackRepository.enqueueNextIfNeeded {
                    generatePlaylist(
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
        if (preferences?.weatherEnabled == false) return weatherRepository.currentWeather.value
        val snapshot = weatherRepository.getWeather().getOrNull()
        if (snapshot != null && _sessionState.value.activity?.weather != snapshot) {
            _sessionState.update { state ->
                state.copy(activity = state.activity?.attachWeather(snapshot))
            }
        }
        return snapshot
    }

    fun pauseSession() = cadenceMeter.stop()

    fun resumeSession() = cadenceMeter.start()

    fun endSession(): Activity? {
        cadenceMeter.stop()
        val finished = _sessionState.value.activity?.finish()
        _sessionState.value = WorkoutSessionSnapshot()
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