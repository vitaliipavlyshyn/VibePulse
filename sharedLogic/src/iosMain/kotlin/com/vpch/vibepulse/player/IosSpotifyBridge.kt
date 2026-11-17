package com.vpch.vibepulse.player

interface IosSpotifyBridge {
    val isConnected: Boolean
    fun connect(clientId: String, redirectUri: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun disconnect()
    fun play(uri: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun queue(uri: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun pause(onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun pauseImmediate()
    fun resume(onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun skipNext(onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun seekTo(positionMs: Long, onSuccess: () -> Unit, onFailure: (String) -> Unit)
}