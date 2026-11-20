package com.vpch.vibepulse.data.playback

import com.vpch.vibepulse.domain.model.PlaybackSnapshot
import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.domain.repository.PlaybackRepository
import com.vpch.vibepulse.player.SpotifyRemoteClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class PlaybackRepositoryImpl(
    private val remoteClient: SpotifyRemoteClient,
    private val remoteGateway: RemotePlaybackGateway,
    private val webApiGateway: WebApiPlaybackGateway
) : PlaybackRepository {

    private val _state = MutableStateFlow(PlaybackSnapshot())
    override val state: StateFlow<PlaybackSnapshot> = _state.asStateFlow()

    private var isCommandInFlight = false
    private var lastQueuedForTrackUri: String? = null
    private var lastPlayStateCommandMark: TimeMark? = null
    private var lastSeekCommandMark: TimeMark? = null

    private val playStateGracePeriod = 2.5.seconds
    private val seekGracePeriod = 2.seconds

    private val queue = ArrayDeque<Track>()
    private val knownTracks = mutableMapOf<String, Track>()

    override suspend fun connectRemote(): Result<Unit> =
        remoteClient.connect().onSuccess {
            _state.update { it.copy(isRemoteConnected = remoteClient.isConnected) }
        }

    override fun disconnectRemote() {
        remoteClient.disconnect()
        _state.update { it.copy(isRemoteConnected = false) }
    }

    override fun resetQueue() {
        queue.clear()
        knownTracks.clear()
        lastQueuedForTrackUri = null
    }

    override suspend fun playBatch(tracks: List<Track>): Result<Unit> {
        if (tracks.isEmpty() || isCommandInFlight) return Result.success(Unit)
        isCommandInFlight = true

        return try {
            tracks.forEach { knownTracks[it.spotifyUri] = it }
            val first = tracks.first()

            val result = executeWithFallback(
                primary = { remoteGateway.playBatch(tracks) },
                fallback = { webApiGateway.playBatch(tracks) }
            )

            result.onSuccess {
                queue.clear()
                queue.addAll(tracks.drop(1))
                lastQueuedForTrackUri = null
                lastPlayStateCommandMark = TimeSource.Monotonic.markNow()
                lastSeekCommandMark = null
                _state.update {
                    it.copy(
                        currentTrack = first,
                        isPlaying = true,
                        positionMs = 0L,
                        durationMs = first.durationMs,
                        isRemoteConnected = remoteClient.isConnected
                    )
                }
            }
        } finally {
            isCommandInFlight = false
        }
    }

    override suspend fun resume(): Result<Unit> {
        if (isCommandInFlight) return Result.success(Unit)
        isCommandInFlight = true

        val wasPlaying = _state.value.isPlaying
        _state.update { it.copy(isPlaying = true) }
        lastPlayStateCommandMark = TimeSource.Monotonic.markNow()

        return try {
            val fallbackQueue = buildList {
                _state.value.currentTrack?.let(::add)
                addAll(queue)
            }
            val result = executeWithFallback(
                primary = { remoteGateway.resume(fallbackQueue) },
                fallback = { webApiGateway.resume(fallbackQueue) }
            )
            result.onFailure {
                _state.update { s -> s.copy(isPlaying = wasPlaying) }
                lastPlayStateCommandMark = null
            }
        } finally {
            isCommandInFlight = false
        }
    }

    override suspend fun pause(): Result<Unit> {
        var waitAttempts = 0
        while (isCommandInFlight && waitAttempts < 10) {
            delay(50.milliseconds)
            waitAttempts++
        }
        isCommandInFlight = true

        val wasPlaying = _state.value.isPlaying
        _state.update { it.copy(isPlaying = false) }
        lastPlayStateCommandMark = TimeSource.Monotonic.markNow()

        return try {
            val result = executeWithFallback(
                primary = { remoteGateway.pause() },
                fallback = { webApiGateway.pause() }
            )
            result.onFailure {
                _state.update { s -> s.copy(isPlaying = wasPlaying) }
                lastPlayStateCommandMark = null
            }
        } finally {
            isCommandInFlight = false
        }
    }

    override fun pauseImmediate() {
        _state.update { it.copy(isPlaying = false) }
        lastPlayStateCommandMark = TimeSource.Monotonic.markNow()
        remoteClient.pauseImmediate()
    }

    override suspend fun skipToNext(): Result<Unit> {
        if (isCommandInFlight) return Result.success(Unit)
        isCommandInFlight = true

        return try {
            val previousUri = _state.value.currentTrack?.spotifyUri
            val nextQueued = queue.firstOrNull()

            val result = executeWithFallback(
                primary = { remoteGateway.skipNext(nextQueued) },
                fallback = { webApiGateway.skipNext(nextQueued) }
            )

            result.onSuccess {
                if (nextQueued != null && queue.firstOrNull()?.spotifyUri == nextQueued.spotifyUri) {
                    queue.removeFirst()
                }
                lastPlayStateCommandMark = TimeSource.Monotonic.markNow()
                lastSeekCommandMark = null
                _state.update {
                    it.copy(
                        currentTrack = nextQueued ?: it.currentTrack,
                        isPlaying = true,
                        positionMs = 0L,
                        durationMs = nextQueued?.durationMs ?: 0L
                    )
                }
                delay(300.milliseconds)
                syncStateInternal(ignoreIsPlaying = true, previousUri = previousUri)
            }
        } finally {
            isCommandInFlight = false
        }
    }

    override suspend fun seekTo(positionMs: Long): Result<Unit> {
        val targetMs = positionMs.coerceAtLeast(0L)
        if (isCommandInFlight) return Result.success(Unit)
        isCommandInFlight = true

        return try {
            val result = executeWithFallback(
                primary = { remoteGateway.seekTo(targetMs) },
                fallback = { webApiGateway.seekTo(targetMs) }
            )
            result.onSuccess {
                lastSeekCommandMark = TimeSource.Monotonic.markNow()
                _state.update { it.copy(positionMs = targetMs) }
            }
        } finally {
            isCommandInFlight = false
        }
    }

    override suspend fun enqueueNextIfNeeded(nextTrackProvider: suspend () -> Track?) {
        val activeUri = _state.value.currentTrack?.spotifyUri ?: return
        if (lastQueuedForTrackUri == activeUri || queue.size >= 2) return

        val nextTrack = nextTrackProvider() ?: return
        val enqueued = if (remoteGateway.isAvailable) {
            remoteGateway.enqueue(nextTrack.spotifyUri).isSuccess
        } else {
            webApiGateway.enqueue(nextTrack.spotifyUri).isSuccess
        }

        if (enqueued) {
            knownTracks[nextTrack.spotifyUri] = nextTrack
            queue.addLast(nextTrack)
            lastQueuedForTrackUri = activeUri
        }
    }

    override suspend fun syncState() {
        if (isCommandInFlight) return
        syncStateInternal(ignoreIsPlaying = false, previousUri = null)
    }

    private suspend fun syncStateInternal(ignoreIsPlaying: Boolean, previousUri: String?) {
        val inPlayStateGrace = ignoreIsPlaying ||
                (lastPlayStateCommandMark?.elapsedNow() ?: playStateGracePeriod) < playStateGracePeriod
        val inSeekGrace =
            (lastSeekCommandMark?.elapsedNow() ?: seekGracePeriod) < seekGracePeriod

        val external = (if (remoteGateway.isAvailable) remoteGateway.fetchState() else null)
            ?: webApiGateway.fetchState()
            ?: return

        val newUri = external.trackUri
        val resolvedTrack = when {
            newUri.isNullOrBlank() -> _state.value.currentTrack
            newUri == previousUri -> _state.value.currentTrack
            newUri != _state.value.currentTrack?.spotifyUri -> {
                trimQueueUpTo(newUri)
                knownTracks[newUri]
                    ?: external.resolvedTrack
                    ?: webApiGateway.fetchState()?.resolvedTrack
                    ?: _state.value.currentTrack
            }
            else -> _state.value.currentTrack
        }

        _state.update { current ->
            current.copy(
                currentTrack = resolvedTrack,
                isPlaying = if (inPlayStateGrace) current.isPlaying else external.isPlaying,
                positionMs = if (inSeekGrace) current.positionMs else external.positionMs,
                durationMs = external.durationMs.takeIf { it > 0L }
                    ?: resolvedTrack?.durationMs
                    ?: current.durationMs,
                isRemoteConnected = remoteClient.isConnected
            )
        }
    }

    private suspend fun executeWithFallback(
        primary: suspend () -> Result<Unit>,
        fallback: suspend () -> Result<Unit>
    ): Result<Unit> {
        val primaryResult = primary()
        if (primaryResult.isSuccess) return primaryResult
        return fallback().recoverCatching { webError ->
            val sdkReason = primaryResult.exceptionOrNull()?.message ?: "unknown"
            throw Exception("SDK: $sdkReason | Web: ${webError.message}")
        }
    }

    private fun trimQueueUpTo(activeUri: String) {
        val idx = queue.indexOfFirst { it.spotifyUri == activeUri }
        if (idx >= 0) {
            repeat(idx + 1) {
                if (queue.isNotEmpty()) queue.removeFirst()
            }
        }
    }

    override fun clearPlaybackState() {
        resetQueue()
        lastPlayStateCommandMark = null
        lastSeekCommandMark = null
        _state.update {
            it.copy(
                currentTrack = null,
                isPlaying = false,
                positionMs = 0L,
                durationMs = 0L
            )
        }
    }
}