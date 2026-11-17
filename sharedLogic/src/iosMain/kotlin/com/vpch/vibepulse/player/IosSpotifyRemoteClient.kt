package com.vpch.vibepulse.player

import com.vpch.vibepulse.network.SpotifyConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class IosSpotifyRemoteClient(
    private val config: SpotifyConfig
) : SpotifyRemoteClient {

    private var bridge: IosSpotifyBridge? = null

    private val _playbackState = MutableStateFlow<RemotePlaybackState?>(null)
    override val playbackState: StateFlow<RemotePlaybackState?> = _playbackState.asStateFlow()

    override val isConnected: Boolean
        get() = bridge?.isConnected == true

    fun attachBridge(swiftBridge: IosSpotifyBridge) {
        this.bridge = swiftBridge
    }

    fun updatePlaybackState(
        trackUri: String,
        title: String,
        artist: String,
        isPaused: Boolean,
        positionMs: Long,
        durationMs: Long
    ) {
        _playbackState.value = RemotePlaybackState(
            trackUri = trackUri,
            title = title,
            artist = artist,
            isPaused = isPaused,
            positionMs = positionMs,
            durationMs = durationMs
        )
    }

    fun clearPlaybackState() {
        _playbackState.value = null
    }

    override suspend fun connect(): Result<Unit> = runCatching {
        if (isConnected) return@runCatching
        val currentBridge = requireBridge()
        awaitBridgeCall { onSuccess, onFailure ->
            currentBridge.connect(config.clientId, config.redirectUri, onSuccess, onFailure)
        }
    }

    override fun disconnect() {
        bridge?.disconnect()
        clearPlaybackState()
    }

    override suspend fun play(uri: String): Result<Unit> = runCatching {
        ensureConnected()
        awaitBridgeCall { onSuccess, onFailure ->
            requireBridge().play(uri, onSuccess, onFailure)
        }
    }

    override suspend fun queue(uri: String): Result<Unit> = runCatching {
        ensureConnected()
        awaitBridgeCall { onSuccess, onFailure ->
            requireBridge().queue(uri, onSuccess, onFailure)
        }
    }

    override suspend fun pause(): Result<Unit> = runCatching {
        ensureConnected()
        awaitBridgeCall { onSuccess, onFailure ->
            requireBridge().pause(onSuccess, onFailure)
        }
    }

    override fun pauseImmediate() {
        if (isConnected) {
            bridge?.pauseImmediate()
        }
    }

    override suspend fun resume(): Result<Unit> = runCatching {
        ensureConnected()
        awaitBridgeCall { onSuccess, onFailure ->
            requireBridge().resume(onSuccess, onFailure)
        }
    }

    override suspend fun skipNext(): Result<Unit> = runCatching {
        ensureConnected()
        awaitBridgeCall { onSuccess, onFailure ->
            requireBridge().skipNext(onSuccess, onFailure)
        }
    }

    override suspend fun seekTo(positionMs: Long): Result<Unit> = runCatching {
        ensureConnected()
        val safePosition = positionMs.coerceAtLeast(0L)
        awaitBridgeCall { onSuccess, onFailure ->
            requireBridge().seekTo(safePosition, onSuccess, onFailure)
        }
    }

    private suspend fun ensureConnected() {
        if (!isConnected) {
            connect().getOrThrow()
        }
    }

    private fun requireBridge(): IosSpotifyBridge =
        bridge ?: throw IllegalStateException("IosSpotifyBridge не підключено у Swift")

    private suspend fun awaitBridgeCall(
        block: (onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit
    ) = suspendCancellableCoroutine { cont ->
        block(
            {
                if (cont.isActive) cont.resume(Unit)
            },
            { errorMessage ->
                if (cont.isActive) cont.resumeWithException(IllegalStateException(errorMessage))
            }
        )
    }
}