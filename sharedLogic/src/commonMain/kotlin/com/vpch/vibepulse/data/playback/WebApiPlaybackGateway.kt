package com.vpch.vibepulse.data.playback

import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.network.SpotifyApiService
import com.vpch.vibepulse.utils.toDomain
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class WebApiPlaybackGateway(
    private val spotifyApiService: SpotifyApiService
) : PlaybackGateway {

    override val isAvailable: Boolean = true

    override suspend fun playBatch(tracks: List<Track>): Result<Unit> {
        if (tracks.isEmpty()) return Result.success(Unit)
        val state = spotifyApiService.getPlaybackState().getOrNull()
        val hasActiveSession = state?.item != null
        val canSkip = state?.actions?.disallows?.skippingNext != true

        return if (hasActiveSession && canSkip) {
            val first = tracks.first()
            spotifyApiService.addToQueue(first.spotifyUri).mapCatching {
                delay(450.milliseconds)
                spotifyApiService.skipToNext().getOrThrow()
                tracks.drop(1).forEach { next -> spotifyApiService.addToQueue(next.spotifyUri) }
            }.recoverCatching {
                spotifyApiService.play(uris = tracks.map { it.spotifyUri }).getOrThrow()
            }
        } else {
            spotifyApiService.play(uris = tracks.map { it.spotifyUri })
        }
    }

    override suspend fun enqueue(trackUri: String): Result<Unit> =
        spotifyApiService.addToQueue(trackUri)

    override suspend fun resume(fallbackTracks: List<Track>): Result<Unit> {
        val state = spotifyApiService.getPlaybackState().getOrNull()
        if (state?.isPlaying == true) return Result.success(Unit)

        val canResume = state?.item != null && state.actions?.disallows?.resuming != true
        if (canResume && spotifyApiService.play().isSuccess) {
            return Result.success(Unit)
        }

        if (fallbackTracks.isEmpty()) {
            return Result.failure(IllegalStateException("Спочатку увімкніть трек у Spotify або почніть рух"))
        }
        return spotifyApiService.play(uris = fallbackTracks.map { it.spotifyUri })
    }

    override suspend fun pause(): Result<Unit> {
        val state = spotifyApiService.getPlaybackState().getOrNull()
        if (state?.isPlaying != true || state.actions?.disallows?.pausing == true) {
            return Result.success(Unit)
        }
        return spotifyApiService.pause()
    }

    override suspend fun skipNext(fallbackTrack: Track?): Result<Unit> {
        val state = spotifyApiService.getPlaybackState().getOrNull()
        val skippingDisallowed = state?.item == null || state.actions?.disallows?.skippingNext == true

        return if (skippingDisallowed) {
            val next = fallbackTrack
                ?: return Result.failure(IllegalStateException("У черзі більше немає треків"))
            spotifyApiService.play(uris = listOf(next.spotifyUri))
        } else {
            spotifyApiService.skipToNext()
        }
    }

    override suspend fun seekTo(positionMs: Long): Result<Unit> =
        spotifyApiService.seekTo(positionMs)

    override suspend fun fetchState(): ExternalPlaybackState? {
        val state = spotifyApiService.getPlaybackState().getOrNull() ?: return null
        val track = state.item?.toDomain()
        return ExternalPlaybackState(
            trackUri = track?.spotifyUri,
            resolvedTrack = track,
            isPlaying = state.isPlaying,
            positionMs = (state.progressMs ?: 0L).coerceAtLeast(0L),
            durationMs = (state.item?.durationMs ?: 0L).coerceAtLeast(0L)
        )
    }
}