package com.vpch.vibepulse.domain.service

import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.network.SpotifyApiService
import com.vpch.vibepulse.player.SpotifyRemoteClient
import com.vpch.vibepulse.utils.toDomain
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class PlaybackController(
    private val spotifyApiService: SpotifyApiService,
    private val spotifyRemoteClient: SpotifyRemoteClient
) {
    var currentTrack: Track? = null
        private set

    var isPlaying: Boolean = false
        private set

    var remainingTrackMs: Long = Long.MAX_VALUE
        private set

    val isRemoteConnected: Boolean
        get() = spotifyRemoteClient.isConnected

    private var isCommandInFlight = false
    private var lastQueuedForTrackUri: String? = null
    private var lastPlayStateCommandMark: TimeMark? = null
    private val playStateGracePeriod = 2.5.seconds

    private val _queue = ArrayDeque<Track>()
    val queue: List<Track> get() = _queue.toList()

    private val knownTracks = mutableMapOf<String, Track>()

    fun resetQueue() {
        _queue.clear()
        knownTracks.clear()
        lastQueuedForTrackUri = null
    }

    suspend fun playBatch(tracks: List<Track>): Result<Unit> {
        if (tracks.isEmpty() || isCommandInFlight) return Result.success(Unit)
        isCommandInFlight = true

        return try {
            tracks.forEach { knownTracks[it.spotifyUri] = it }
            val first = tracks.first()

            val remoteResult = spotifyRemoteClient.play(first.spotifyUri).onSuccess {
                tracks.drop(1).forEach { next ->
                    spotifyRemoteClient.queue(next.spotifyUri)
                }
            }

            val result = if (remoteResult.isSuccess) {
                remoteResult
            } else {
                playBatchViaWebApi(tracks).recoverCatching { webError ->
                    val sdkReason = remoteResult.exceptionOrNull()?.message ?: "unknown"
                    throw Exception("SDK: $sdkReason | Web: ${webError.message}")
                }
            }

            result.onSuccess {
                currentTrack = first
                _queue.clear()
                _queue.addAll(tracks.drop(1))
                isPlaying = true
                lastQueuedForTrackUri = null
                remainingTrackMs = Long.MAX_VALUE
                lastPlayStateCommandMark = TimeSource.Monotonic.markNow()
            }
        } finally {
            isCommandInFlight = false
        }
    }

    suspend fun resume(): Result<Unit> {
        if (isCommandInFlight) return Result.success(Unit)
        isCommandInFlight = true

        return try {
            val activeTrack = currentTrack
            val remoteResult = if (activeTrack != null) {
                spotifyRemoteClient.play(activeTrack.spotifyUri).onSuccess {
                    _queue.forEach { spotifyRemoteClient.queue(it.spotifyUri) }
                }
            } else {
                spotifyRemoteClient.resume()
            }

            val result = if (remoteResult.isSuccess) {
                remoteResult
            } else {
                resumeViaWebApi()
            }

            result.onSuccess {
                isPlaying = true
                lastPlayStateCommandMark = TimeSource.Monotonic.markNow()
            }
        } finally {
            isCommandInFlight = false
        }
    }

    suspend fun enqueueNextIfNeeded(nextTrackProvider: suspend () -> Track?) {
        val activeUri = currentTrack?.spotifyUri ?: return
        if (lastQueuedForTrackUri == activeUri || _queue.size >= 2) return

        val nextTrack = nextTrackProvider() ?: return
        val queued = if (spotifyRemoteClient.isConnected) {
            spotifyRemoteClient.queue(nextTrack.spotifyUri).isSuccess
        } else {
            spotifyApiService.addToQueue(nextTrack.spotifyUri).isSuccess
        }

        if (queued) {
            knownTracks[nextTrack.spotifyUri] = nextTrack
            _queue.addLast(nextTrack)
            lastQueuedForTrackUri = activeUri
        }
    }

    suspend fun skipToNext(): Result<Unit> {
        if (isCommandInFlight) return Result.success(Unit)
        isCommandInFlight = true

        return try {
            val previousUri = currentTrack?.spotifyUri

            val result = if (spotifyRemoteClient.isConnected) {
                spotifyRemoteClient.skipNext()
                    .recoverCatching {
                        val fallback = _queue.removeFirstOrNull()
                            ?: throw IllegalStateException("У черзі більше немає треків")
                        spotifyRemoteClient.play(fallback.spotifyUri).getOrThrow()
                        currentTrack = fallback
                    }
            } else {
                skipToNextViaWebApi()
            }

            result.onSuccess {
                isPlaying = true
                remainingTrackMs = Long.MAX_VALUE
                lastPlayStateCommandMark = TimeSource.Monotonic.markNow()

                // Чекаємо коротку затримку, щоб Spotify перемкнув трек, і одразу читаємо реальну назву
                delay(300.milliseconds)
                refreshActualTrack(previousUri)
            }
        } finally {
            isCommandInFlight = false
        }
    }

    suspend fun syncPlaybackState() {
        if (isCommandInFlight) return
        val inPlayStateGrace = (lastPlayStateCommandMark?.elapsedNow() ?: playStateGracePeriod) < playStateGracePeriod

        val remoteState = spotifyRemoteClient.playbackState.value
        if (spotifyRemoteClient.isConnected && remoteState != null) {
            if (!inPlayStateGrace) {
                isPlaying = !remoteState.isPaused
            }

            remainingTrackMs = if (remoteState.durationMs > 0) {
                (remoteState.durationMs - remoteState.positionMs).coerceAtLeast(0L)
            } else {
                Long.MAX_VALUE
            }

            if (remoteState.trackUri.isNotBlank() && remoteState.trackUri != currentTrack?.spotifyUri) {
                applyTrackUriChange(remoteState.trackUri)
            }
            return
        }

        syncStateViaWebApi(ignoreIsPlaying = inPlayStateGrace)
    }

    suspend fun pause(): Result<Unit> {
        if (isCommandInFlight) return Result.success(Unit)
        isCommandInFlight = true

        return try {
            val result = if (spotifyRemoteClient.isConnected) {
                spotifyRemoteClient.pause().recoverCatching {
                    pauseViaWebApi().getOrThrow()
                }
            } else {
                pauseViaWebApi()
            }

            result.onSuccess {
                isPlaying = false
                lastPlayStateCommandMark = TimeSource.Monotonic.markNow()
            }
        } finally {
            isCommandInFlight = false
        }
    }

    fun pauseImmediate() {
        isPlaying = false
        lastPlayStateCommandMark = TimeSource.Monotonic.markNow()
        spotifyRemoteClient.pauseImmediate()
    }

    suspend fun connectRemote(): Result<Unit> = spotifyRemoteClient.connect()

    fun disconnectRemote() {
        spotifyRemoteClient.disconnect()
    }

    private suspend fun refreshActualTrack(previousUri: String?) {
        val remoteState = spotifyRemoteClient.playbackState.value
        if (spotifyRemoteClient.isConnected &&
            remoteState != null &&
            remoteState.trackUri.isNotBlank() &&
            remoteState.trackUri != previousUri
        ) {
            applyTrackUriChange(remoteState.trackUri)
            return
        }

        syncStateViaWebApi(ignoreIsPlaying = true)
    }

    private suspend fun applyTrackUriChange(newUri: String) {
        trimQueueUpTo(newUri)
        val cached = knownTracks[newUri]
        if (cached != null) {
            currentTrack = cached
        } else {
            syncStateViaWebApi(ignoreIsPlaying = true)
        }
    }

    private fun trimQueueUpTo(activeUri: String) {
        val idx = _queue.indexOfFirst { it.spotifyUri == activeUri }
        if (idx >= 0) {
            repeat(idx + 1) {
                if (_queue.isNotEmpty()) _queue.removeFirst()
            }
        }
    }

    private suspend fun playBatchViaWebApi(tracks: List<Track>): Result<Unit> {
        val state = spotifyApiService.getPlaybackState().getOrNull()
        val hasActiveSession = state?.item != null
        val canSkip = state?.actions?.disallows?.skippingNext != true

        return if (hasActiveSession && canSkip) {
            val first = tracks.first()
            spotifyApiService.addToQueue(first.spotifyUri).mapCatching {
                delay(450.milliseconds)
                spotifyApiService.skipToNext().getOrThrow()
                tracks.drop(1).forEach { next ->
                    spotifyApiService.addToQueue(next.spotifyUri)
                }
            }.recoverCatching {
                spotifyApiService.play(uris = tracks.map { it.spotifyUri }).getOrThrow()
            }
        } else {
            spotifyApiService.play(uris = tracks.map { it.spotifyUri })
        }
    }

    private suspend fun skipToNextViaWebApi(): Result<Unit> {
        val state = spotifyApiService.getPlaybackState().getOrNull()
        val skippingDisallowed = state?.item == null || state.actions?.disallows?.skippingNext == true

        return if (skippingDisallowed) {
            val fallback = _queue.removeFirstOrNull()
                ?: return Result.failure(IllegalStateException("У черзі більше немає треків"))
            spotifyApiService.play(uris = listOf(fallback.spotifyUri)).onSuccess {
                currentTrack = fallback
            }
        } else {
            spotifyApiService.skipToNext()
        }
    }

    private suspend fun syncStateViaWebApi(ignoreIsPlaying: Boolean = false) {
        spotifyApiService.getPlaybackState().onSuccess { state ->
            val remoteTrack = state?.item?.toDomain()
            if (remoteTrack != null && remoteTrack.spotifyUri != currentTrack?.spotifyUri) {
                trimQueueUpTo(remoteTrack.spotifyUri)
                currentTrack = knownTracks[remoteTrack.spotifyUri] ?: remoteTrack
            }

            if (!ignoreIsPlaying) {
                isPlaying = state?.isPlaying ?: false
            }

            val duration = state?.item?.durationMs ?: 0L
            val progress = state?.progressMs ?: 0L
            remainingTrackMs = if (duration > 0) (duration - progress).coerceAtLeast(0L) else Long.MAX_VALUE
        }
    }

    private suspend fun pauseViaWebApi(): Result<Unit> {
        val state = spotifyApiService.getPlaybackState().getOrNull()
        if (state?.isPlaying != true || state.actions?.disallows?.pausing == true) {
            return Result.success(Unit)
        }
        return spotifyApiService.pause()
    }

    private suspend fun resumeViaWebApi(): Result<Unit> {
        val state = spotifyApiService.getPlaybackState().getOrNull()
        if (state?.isPlaying == true) {
            currentTrack = state.item?.toDomain() ?: currentTrack
            return Result.success(Unit)
        }

        val canResumeContext = state?.item != null && state.actions?.disallows?.resuming != true
        if (canResumeContext) {
            val resumeResult = spotifyApiService.play()
            if (resumeResult.isSuccess) return resumeResult
        }

        val activeTrack = currentTrack
            ?: return Result.failure(IllegalStateException("Спочатку увімкніть трек у Spotify або почніть рух"))

        val uris = listOf(activeTrack.spotifyUri) + _queue.map { it.spotifyUri }
        return spotifyApiService.play(uris = uris).onSuccess {
            remainingTrackMs = Long.MAX_VALUE
        }
    }
}