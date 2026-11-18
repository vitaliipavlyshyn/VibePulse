package com.vpch.vibepulse.screens.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import coil3.compose.AsyncImage
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.OpenSpotifyTrackDialog
import com.vpch.vibepulse.ui.components.button.VibePulseButton
import com.vpch.vibepulse.ui.components.VibePulseCard
import com.vpch.vibepulse.ui.components.VibePulseIconChip
import com.vpch.vibepulse.ui.components.VibePulsePillBadge
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText
import com.vpch.vibepulse.utils.formatMillisToClock
import openTrackInSpotifyOrBrowser

@Composable
internal fun SpotifyPlayerCard(
    modifier: Modifier = Modifier,
    track: Track?,
    isPlaying: Boolean,
    isLiked: Boolean,
    positionMs: Long = 0L,
    durationMs: Long = 0L,
    onToggleLike: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekTo: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val totalDuration = durationMs.takeIf { it > 0L } ?: track?.durationMs ?: 0L
    val isActivelyPlaying = isPlaying && track != null
    var showOpenSpotifyDialog by remember { mutableStateOf(false) }

    if (showOpenSpotifyDialog && track != null && track.spotifyUri.isNotBlank()) {
        OpenSpotifyTrackDialog(
            title = track.title,
            artist = track.artist,
            albumArtUrl = track.albumArtUrl,
            cadenceOrTempo = track.tempo,
            isTempoBpm = true,
            onConfirm = {
                openTrackInSpotifyOrBrowser(context, track.spotifyUri)
                showOpenSpotifyDialog = false
            },
            onDismiss = { showOpenSpotifyDialog = false }
        )
    }

    VibePulseCard(
        modifier = modifier,
        shape = RoundedCornerShape(PlayerDefaults.CardCornerRadius),
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrackArtworkSection(
                albumArtUrl = track?.albumArtUrl,
                title = track?.title,
                tempo = track?.tempo ?: 0,
                isActivelyPlaying = isActivelyPlaying,
                onOpenInSpotify = track?.spotifyUri?.takeIf { it.isNotBlank() }?.let {
                    { showOpenSpotifyDialog = true }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            TrackInfoSection(title = track?.title, artist = track?.artist)

            Spacer(modifier = Modifier.height(10.dp))

            TrackProgressSection(
                positionMs = positionMs,
                totalDurationMs = totalDuration,
                canSeek = track != null && totalDuration > 0L,
                isActivelyPlaying = isActivelyPlaying,
                onSeekTo = onSeekTo
            )

            Spacer(modifier = Modifier.height(8.dp))

            PlayerControlsSection(
                hasTrack = track != null,
                isPlaying = isPlaying,
                isLiked = isLiked,
                onToggleLike = onToggleLike,
                onTogglePlayPause = onTogglePlayPause,
                onSkipNext = onSkipNext
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrackArtworkSection(
    albumArtUrl: String?,
    title: String?,
    tempo: Int,
    isActivelyPlaying: Boolean,
    onOpenInSpotify: (() -> Unit)? = null
) {
    val expressivePolygons = remember {
        listOf(
            MaterialShapes.Square,
            MaterialShapes.Cookie4Sided,
            MaterialShapes.Circle,
            MaterialShapes.Slanted,
            MaterialShapes.Cookie6Sided
        )
    }
    val morphs = remember(expressivePolygons) {
        expressivePolygons.indices.map { i ->
            Morph(expressivePolygons[i], expressivePolygons[(i + 1) % expressivePolygons.size])
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "artworkMorphTransition")
    val cycleProgressState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = morphs.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(morphs.size * PlayerDefaults.MORPH_STEP_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cycleProgress"
    )
    val subtleTiltState = infiniteTransition.animateFloat(
        initialValue = -PlayerDefaults.TILT_DEGREES,
        targetValue = PlayerDefaults.TILT_DEGREES,
        animationSpec = infiniteRepeatable(
            animation = tween(PlayerDefaults.TILT_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "subtleTilt"
    )
    val activeStrengthState = animateFloatAsState(
        targetValue = if (isActivelyPlaying) 1f else 0f,
        animationSpec = tween(PlayerDefaults.STATE_TRANSITION_MS, easing = FastOutSlowInEasing),
        label = "activeStrength"
    )

    val pausedShape = remember { RoundedCornerShape(PlayerDefaults.PausedArtworkCornerRadius) }
    val morphShape = remember(morphs) { ExpressiveMorphShape(morphs, cycleProgressState) }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (tempo > 0) {
            VibePulsePillBadge(
                text = stringResource(R.string.player_bpm_badge, tempo),
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        MorphingArtworkBox(
            albumArtUrl = albumArtUrl,
            title = title,
            isActivelyPlaying = isActivelyPlaying,
            activeStrengthState = activeStrengthState,
            subtleTiltState = subtleTiltState,
            morphShape = morphShape,
            pausedShape = pausedShape
        )

        if (onOpenInSpotify != null) {
            VibePulseIconChip(
                iconRes = R.drawable.spotify_logo,
                contentDescription = stringResource(R.string.player_open_in_spotify),
                onClick = onOpenInSpotify,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun MorphingArtworkBox(
    albumArtUrl: String?,
    title: String?,
    isActivelyPlaying: Boolean,
    activeStrengthState: State<Float>,
    subtleTiltState: State<Float>,
    morphShape: Shape,
    pausedShape: Shape
) {
    val activeShape = if (activeStrengthState.value > 0.01f) morphShape else pausedShape
    val counterRotation = if (isActivelyPlaying) -subtleTiltState.value else 0f

    Box(
        modifier = Modifier
            .size(PlayerDefaults.ArtworkSize)
            .graphicsLayer { rotationZ = if (isActivelyPlaying) subtleTiltState.value else 0f }
            .clip(activeShape)
            .background(Color(0xFF101214)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.spotify_logo),
            contentDescription = null,
            modifier = Modifier
                .size(PlayerDefaults.PlaceholderLogoSize)
                .graphicsLayer { rotationZ = counterRotation },
            tint = Color.White.copy(alpha = 0.25f)
        )

        if (!albumArtUrl.isNullOrBlank()) {
            AsyncImage(
                model = albumArtUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = 1f + (PlayerDefaults.ARTWORK_ACTIVE_SCALE - 1f) * activeStrengthState.value
                        rotationZ = counterRotation
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
}

@Composable
private fun TrackInfoSection(title: String?, artist: String?) {
    val marqueeModifier = Modifier
        .padding(horizontal = 8.dp)
        .basicMarquee(iterations = Int.MAX_VALUE)

    TitleText(
        text = title ?: stringResource(PlayerDefaults.DefaultTrackTitleRes),
        color = AppTheme.colors.textPrimary,
        style = AppTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = marqueeModifier
    )
    Spacer(modifier = Modifier.height(2.dp))
    BodyText(
        text = artist ?: stringResource(PlayerDefaults.DefaultTrackArtistRes),
        color = AppTheme.colors.textSecondary,
        style = AppTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = marqueeModifier
    )
}

@Composable
private fun TrackProgressSection(
    positionMs: Long,
    totalDurationMs: Long,
    canSeek: Boolean,
    isActivelyPlaying: Boolean,
    onSeekTo: (Long) -> Unit
) {
    val playbackProgress = if (totalDurationMs > 0L) {
        (positionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val displayedProgress = if (isDragging) dragProgress else playbackProgress
    val displayedPositionMs = if (isDragging) (dragProgress * totalDurationMs).toLong() else positionMs
    val waveAmplitude by animateFloatAsState(
        targetValue = if (isActivelyPlaying && !isDragging) 1f else 0f,
        label = "waveAmplitude"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlayerDefaults.SeekbarHeight)
                .then(
                    if (canSeek) {
                        Modifier.pointerInput(totalDurationMs) {
                            fun fractionFromX(x: Float) = (x / size.width.toFloat()).coerceIn(0f, 1f)
                            detectHorizontalDragGestures(
                                onDragStart = { isDragging = true; dragProgress = fractionFromX(it.x) },
                                onHorizontalDrag = { change, _ -> change.consume(); dragProgress = fractionFromX(change.position.x) },
                                onDragEnd = { onSeekTo((dragProgress * totalDurationMs).toLong()); isDragging = false },
                                onDragCancel = { isDragging = false }
                            )
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            LinearWavyProgressIndicator(
                progress = { displayedProgress },
                modifier = Modifier.fillMaxWidth(),
                color = AppTheme.colors.indicator,
                trackColor = AppTheme.colors.trackIndicator.copy(alpha = 0.35f),
                amplitude = { waveAmplitude },
                wavelength = PlayerDefaults.WaveLength,
                waveSpeed = PlayerDefaults.WaveSpeed
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BodyText(
                text = formatMillisToClock(displayedPositionMs),
                color = if (isDragging) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                style = AppTheme.typography.labelSmall,
                fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Normal
            )
            BodyText(
                text = formatMillisToClock(totalDurationMs),
                color = AppTheme.colors.textSecondary,
                style = AppTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun PlayerControlsSection(
    hasTrack: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilledTonalIconButton(
            onClick = onToggleLike,
            enabled = hasTrack,
            modifier = Modifier.size(PlayerDefaults.ControlButtonSize),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isLiked) AppTheme.colors.buttonFilled.copy(alpha = 0.18f) else AppTheme.colors.buttonSecondary,
                contentColor = AppTheme.colors.indicator,
                disabledContainerColor = AppTheme.colors.buttonSecondary.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(if (isLiked) R.drawable.fill_favorite else R.drawable.outline_favorite),
                contentDescription = stringResource(if (isLiked) R.string.player_remove_from_favorites else R.string.player_add_to_favorites),
                tint = if (isLiked) AppTheme.colors.indicator else AppTheme.colors.iconSecondary
            )
        }

        VibePulseButton(
            onClick = onTogglePlayPause,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
            modifier = Modifier.height(PlayerDefaults.PlayButtonHeight)
        ) {
            PlayPauseGlyph(isPlaying = isPlaying, color = AppTheme.colors.iconFilled, modifier = Modifier.size(20.dp))
        }

        FilledTonalIconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(PlayerDefaults.ControlButtonSize),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = AppTheme.colors.buttonSecondary,
                contentColor = AppTheme.colors.indicator
            )
        ) {
            SkipNextGlyph(color = AppTheme.colors.iconPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

private class ExpressiveMorphShape(
    private val morphs: List<Morph>,
    private val cycleProgressState: State<Float>
) : Shape {
    private val matrix = Matrix()
    private val androidPath = android.graphics.Path()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cycle = cycleProgressState.value
        val index = cycle.toInt() % morphs.size
        androidPath.rewind()
        morphs[index].toPath(progress = cycle - cycle.toInt(), path = androidPath)
        val composePath = androidPath.asComposePath()
        matrix.reset()
        matrix.scale(size.width, size.height)
        composePath.transform(matrix)
        return Outline.Generic(composePath)
    }
}

@Composable
private fun PlayPauseGlyph(isPlaying: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (isPlaying) {
            val barWidth = size.width * 0.26f
            val barHeight = size.height * 0.8f
            val top = (size.height - barHeight) / 2f
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
            drawRoundRect(color, Offset(size.width * 0.18f, top), Size(barWidth, barHeight), radius)
            drawRoundRect(color, Offset(size.width * 0.56f, top), Size(barWidth, barHeight), radius)
        } else {
            drawPath(
                Path().apply {
                    moveTo(size.width * 0.24f, size.height * 0.14f)
                    lineTo(size.width * 0.86f, size.height * 0.5f)
                    lineTo(size.width * 0.24f, size.height * 0.86f)
                    close()
                },
                color = color
            )
        }
    }
}

@Composable
private fun SkipNextGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawPath(
            Path().apply {
                moveTo(size.width * 0.16f, size.height * 0.18f)
                lineTo(size.width * 0.66f, size.height * 0.5f)
                lineTo(size.width * 0.16f, size.height * 0.82f)
                close()
            },
            color = color
        )
        val barWidth = size.width * 0.14f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.72f, size.height * 0.18f),
            size = Size(barWidth, size.height * 0.64f),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}

@PreviewLightDark
@Composable
private fun SpotifyPlayerCardPreview() = VibePulsePreview {
    SpotifyPlayerCard(
        track = Track(
            spotifyUri = "spotify:track:1",
            title = "Just Keep Watching (From F1 The Album)",
            artist = "Tate McRae, F1 The Album",
            genre = "pop",
            tempo = 128,
            durationMs = 142_000L
        ),
        isPlaying = true,
        isLiked = true,
        positionMs = 12_000L,
        durationMs = 142_000L,
        onToggleLike = {},
        onTogglePlayPause = {},
        onSkipNext = {},
        onSeekTo = {}
    )
}