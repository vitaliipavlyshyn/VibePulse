package com.vpch.vibepulse.data.playback

import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.player.SpotifyRemoteClient

class RemotePlaybackGateway(
    private val remoteClient: SpotifyRemoteClient
) : PlaybackGateway {

    override val isAvailable: Boolean
        get() = remoteClient.isConnected

    override suspend fun playBatch(tracks: List<Track>): Result<Unit> {
        val first = tracks.firstOrNull() ?: return Result.success(Unit)
        return remoteClient.play(first.spotifyUri).onSuccess {
            tracks.drop(1).forEach { next -> remoteClient.queue(next.spotifyUri) }
        }
    }

    override suspend fun enqueue(trackUri: String): Result<Unit> =
        remoteClient.queue(trackUri)

    override suspend fun resume(fallbackTracks: List<Track>): Result<Unit> {
        val activeTrack = fallbackTracks.firstOrNull()
        val loadedUri = remoteClient.playbackState.value?.trackUri
        val isSameTrack = activeTrack == null || loadedUri == activeTrack.spotifyUri

        return if (isSameTrack) {
            remoteClient.resume().recoverCatching {
                if (activeTrack != null) {
                    playBatch(fallbackTracks).getOrThrow()
                } else {
                    throw it
                }
            }
        } else {
            playBatch(fallbackTracks)
        }
    }

    override suspend fun pause(): Result<Unit> = remoteClient.pause()

    override suspend fun skipNext(fallbackTrack: Track?): Result<Unit> =
        remoteClient.skipNext().recoverCatching {
            val next = fallbackTrack ?: throw IllegalStateException("У черзі більше немає треків")
            remoteClient.play(next.spotifyUri).getOrThrow()
        }

    override suspend fun seekTo(positionMs: Long): Result<Unit> =
        remoteClient.seekTo(positionMs)

    override suspend fun fetchState(): ExternalPlaybackState? {
        val remote = remoteClient.playbackState.value ?: return null
        return ExternalPlaybackState(
            trackUri = remote.trackUri.takeIf { it.isNotBlank() },
            resolvedTrack = null,
            isPlaying = !remote.isPaused,
            positionMs = remote.positionMs.coerceAtLeast(0L),
            durationMs = remote.durationMs.coerceAtLeast(0L)
        )
    }
}