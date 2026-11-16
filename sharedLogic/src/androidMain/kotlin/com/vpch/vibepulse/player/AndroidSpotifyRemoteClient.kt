package com.vpch.vibepulse.player

import android.content.Context
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.CallResult
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import com.vpch.vibepulse.network.SpotifyConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidSpotifyRemoteClient(
    context: Context,
    private val config: SpotifyConfig
) : SpotifyRemoteClient {

    private val appContext = context.applicationContext
    private var appRemote: SpotifyAppRemote? = null
    private var playerStateSubscription: Subscription<PlayerState>? = null

    private val _playbackState = MutableStateFlow<RemotePlaybackState?>(null)
    override val playbackState: StateFlow<RemotePlaybackState?> = _playbackState.asStateFlow()

    override val isConnected: Boolean
        get() = appRemote?.isConnected == true

    override suspend fun connect(): Result<Unit> = runCatching {
        if (isConnected) return@runCatching

        val connectionParams = ConnectionParams.Builder(config.clientId)
            .setRedirectUri(config.redirectUri)
            .showAuthView(true)
            .build()

        val remote = suspendCancellableCoroutine { cont ->
            SpotifyAppRemote.connect(
                appContext,
                connectionParams,
                object : Connector.ConnectionListener {
                    override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                        appRemote = spotifyAppRemote
                        subscribeToPlayerState(spotifyAppRemote)
                        if (cont.isActive) {
                            cont.resume(spotifyAppRemote)
                        }
                    }

                    override fun onFailure(throwable: Throwable) {
                        cleanupConnection()
                        if (cont.isActive) {
                            cont.resumeWithException(throwable)
                        }
                    }
                }
            )
        }
        appRemote = remote
    }

    override fun disconnect() {
        cleanupConnection()
    }

    override suspend fun play(uri: String): Result<Unit> = runCatching {
        val remote = ensureConnected()
        remote.playerApi.play(uri).await()
    }

    override suspend fun queue(uri: String): Result<Unit> = runCatching {
        val remote = ensureConnected()
        remote.playerApi.queue(uri).await()
    }

    override suspend fun pause(): Result<Unit> = runCatching {
        val remote = ensureConnected()
        remote.playerApi.pause().await()
    }

    override fun pauseImmediate() {
        appRemote?.takeIf { it.isConnected }?.playerApi?.pause()
    }

    override suspend fun resume(): Result<Unit> = runCatching {
        val remote = ensureConnected()
        remote.playerApi.resume().await()
    }

    override suspend fun skipNext(): Result<Unit> = runCatching {
        val remote = ensureConnected()
        remote.playerApi.skipNext().await()
    }

    private suspend fun ensureConnected(): SpotifyAppRemote {
        appRemote?.takeIf { it.isConnected }?.let { return it }
        connect().getOrThrow()
        return appRemote ?: throw IllegalStateException("Spotify App Remote не підключено")
    }

    private fun subscribeToPlayerState(remote: SpotifyAppRemote) {
        playerStateSubscription?.cancel()
        playerStateSubscription = remote.playerApi
            .subscribeToPlayerState()
            .setEventCallback { state ->
                val track = state.track
                _playbackState.value = if (track != null) {
                    RemotePlaybackState(
                        trackUri = track.uri.orEmpty(),
                        title = track.name.orEmpty(),
                        artist = track.artist?.name.orEmpty(),
                        isPaused = state.isPaused,
                        positionMs = state.playbackPosition,
                        durationMs = track.duration
                    )
                } else {
                    null
                }
            }
    }

    private fun cleanupConnection() {
        playerStateSubscription?.cancel()
        playerStateSubscription = null
        appRemote?.let { SpotifyAppRemote.disconnect(it) }
        appRemote = null
        _playbackState.value = null
    }

    private suspend fun <T> CallResult<T>.await(): T = suspendCancellableCoroutine { cont ->
        setResultCallback { result ->
            if (cont.isActive) cont.resume(result)
        }
        setErrorCallback { throwable ->
            if (cont.isActive) cont.resumeWithException(throwable)
        }
        cont.invokeOnCancellation {
            cancel()
        }
    }


    override suspend fun seekTo(positionMs: Long): Result<Unit> = runCatching {
        val remote = ensureConnected()
        remote.playerApi.seekTo(positionMs.coerceAtLeast(0L)).await()
    }
}