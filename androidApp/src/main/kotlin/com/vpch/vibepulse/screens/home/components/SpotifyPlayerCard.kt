package com.vpch.vibepulse.screens.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
import com.vpch.vibepulse.ui.VibePulseTheme
import com.vpch.vibepulse.ui.components.VibePulseButton
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText

private object PlayerDefaults {
    val CardCornerRadius = 28.dp
    val ArtworkSize = 188.dp
    val PausedArtworkCornerRadius = 24.dp
    val PlaceholderLogoSize = 58.dp
    val ControlButtonSize = 44.dp
    val PlayButtonHeight = 48.dp
    val SeekbarHeight = 26.dp
    val WaveLength = 24.dp
    val WaveSpeed = 16.dp

    const val MORPH_STEP_DURATION_MS = 2400
    const val TILT_DURATION_MS = 3200
    const val STATE_TRANSITION_MS = 500
    const val TILT_DEGREES = 3.5f
    const val ARTWORK_ACTIVE_SCALE = 1.06f

    val DefaultTrackTitleRes = R.string.no_active_track
    val DefaultTrackArtistRes = R.string.play_music_on_spotify
}

@Composable
fun SpotifyPlayerCard(
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
    val totalDuration = durationMs.takeIf { it > 0L } ?: track?.durationMs ?: 0L
    val isActivelyPlaying = isPlaying && track != null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PlayerDefaults.CardCornerRadius),
        color = AppTheme.colors.buttonPrimary,
        border = BorderStroke(
            width = 1.5.dp,
            color = AppTheme.colors.strokePrimary.copy(alpha = 0.55f)
        ),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrackArtworkSection(
                albumArtUrl = track?.albumArtUrl,
                title = track?.title,
                tempo = track?.tempo ?: 0,
                isActivelyPlaying = isActivelyPlaying
            )

            Spacer(modifier = Modifier.height(10.dp))

            TrackInfoSection(
                title = track?.title,
                artist = track?.artist
            )

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
    isActivelyPlaying: Boolean
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
            val next = (i + 1) % expressivePolygons.size
            Morph(expressivePolygons[i], expressivePolygons[next])
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "artworkMorphTransition")
    val cycleProgressState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = morphs.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = morphs.size * PlayerDefaults.MORPH_STEP_DURATION_MS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "cycleProgress"
    )
    val subtleTiltState = infiniteTransition.animateFloat(
        initialValue = -PlayerDefaults.TILT_DEGREES,
        targetValue = PlayerDefaults.TILT_DEGREES,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = PlayerDefaults.TILT_DURATION_MS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "subtleTilt"
    )
    val activeStrengthState = animateFloatAsState(
        targetValue = if (isActivelyPlaying) 1f else 0f,
        animationSpec = tween(
            durationMillis = PlayerDefaults.STATE_TRANSITION_MS,
            easing = FastOutSlowInEasing
        ),
        label = "activeStrength"
    )

    val pausedShape = remember { RoundedCornerShape(PlayerDefaults.PausedArtworkCornerRadius) }
    val morphShape = remember(morphs) {
        ExpressiveMorphShape(
            morphs = morphs,
            cycleProgressState = cycleProgressState
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        MorphingArtworkBox(
            albumArtUrl = albumArtUrl,
            title = title,
            isActivelyPlaying = isActivelyPlaying,
            activeStrengthState = activeStrengthState,
            subtleTiltState = subtleTiltState,
            morphShape = morphShape,
            pausedShape = pausedShape
        )

        if (tempo > 0) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = CircleShape,
                color = AppTheme.colors.buttonSecondary,
                border = BorderStroke(1.dp, AppTheme.colors.strokePrimary.copy(alpha = 0.4f))
            ) {
                BodyText(
                    text = "$tempo BPM",
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
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

    Box(
        modifier = Modifier
            .size(PlayerDefaults.ArtworkSize)
            .graphicsLayer {
                rotationZ = if (isActivelyPlaying) subtleTiltState.value else 0f
            }
            .clip(activeShape)
            .background(Color(0xFF101214)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.spotify_logo),
            contentDescription = null,
            modifier = Modifier
                .size(PlayerDefaults.PlaceholderLogoSize)
                .graphicsLayer {
                    rotationZ = if (isActivelyPlaying) -subtleTiltState.value else 0f
                },
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
                        val strength = activeStrengthState.value
                        rotationZ = if (isActivelyPlaying) -subtleTiltState.value else 0f
                        val scale = 1f + (PlayerDefaults.ARTWORK_ACTIVE_SCALE - 1f) * strength
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
}

@Composable
private fun TrackInfoSection(
    title: String?,
    artist: String?
) {
    TitleText(
        text = title ?: stringResource(PlayerDefaults.DefaultTrackTitleRes),
        color = AppTheme.colors.textPrimary,
        style = AppTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .basicMarquee(iterations = Int.MAX_VALUE)
    )

    Spacer(modifier = Modifier.height(2.dp))

    BodyText(
        text = artist ?: stringResource(PlayerDefaults.DefaultTrackArtistRes),
        color = AppTheme.colors.textSecondary,
        style = AppTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .basicMarquee(iterations = Int.MAX_VALUE)
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
    } else {
        0f
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val displayedProgress = if (isDragging) dragProgress else playbackProgress
    val displayedPositionMs = if (isDragging) {
        (dragProgress * totalDurationMs).toLong()
    } else {
        positionMs
    }

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
                            fun fractionFromX(x: Float): Float =
                                (x / size.width.toFloat()).coerceIn(0f, 1f)

                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragProgress = fractionFromX(offset.x)
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    dragProgress = fractionFromX(change.position.x)
                                },
                                onDragEnd = {
                                    onSeekTo((dragProgress * totalDurationMs).toLong())
                                    isDragging = false
                                },
                                onDragCancel = {
                                    isDragging = false
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BodyText(
                text = formatTrackTime(displayedPositionMs),
                color = if (isDragging) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                style = AppTheme.typography.labelSmall,
                fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Normal
            )
            BodyText(
                text = formatTrackTime(totalDurationMs),
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
                containerColor = if (isLiked) {
                    AppTheme.colors.buttonFilled.copy(alpha = 0.18f)
                } else {
                    AppTheme.colors.buttonSecondary
                },
                contentColor = AppTheme.colors.indicator,
                disabledContainerColor = AppTheme.colors.buttonSecondary.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                imageVector = if (isLiked) {
                    ImageVector.vectorResource(R.drawable.fill_favorite)
                } else {
                    ImageVector.vectorResource(R.drawable.outline_favorite)
                },
                contentDescription = if (isLiked) "Видалити з улюблених" else "Додати в улюблені",
                tint = if (isLiked) {
                    AppTheme.colors.indicator
                } else {
                    AppTheme.colors.iconSecondary
                }
            )
        }

        VibePulseButton(
            onClick = onTogglePlayPause,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
            modifier = Modifier.height(PlayerDefaults.PlayButtonHeight)
        ) {
            PlayPauseGlyph(
                isPlaying = isPlaying,
                color = AppTheme.colors.iconFilled,
                modifier = Modifier.size(20.dp)
            )
        }

        FilledTonalIconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(PlayerDefaults.ControlButtonSize),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = AppTheme.colors.buttonSecondary,
                contentColor = AppTheme.colors.indicator
            )
        ) {
            SkipNextGlyph(
                color = AppTheme.colors.iconPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private class ExpressiveMorphShape(
    private val morphs: List<Morph>,
    private val cycleProgressState: State<Float>
) : Shape {
    private val matrix = Matrix()
    private val androidPath = android.graphics.Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cycle = cycleProgressState.value
        val index = cycle.toInt() % morphs.size
        val fraction = cycle - cycle.toInt()

        androidPath.rewind()
        morphs[index].toPath(progress = fraction, path = androidPath)

        val composePath = androidPath.asComposePath()
        matrix.reset()
        matrix.scale(size.width, size.height)
        composePath.transform(matrix)
        return Outline.Generic(composePath)
    }
}

@Composable
private fun PlayPauseGlyph(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (isPlaying) {
            val barWidth = size.width * 0.26f
            val barHeight = size.height * 0.8f
            val top = (size.height - barHeight) / 2f
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

            drawRoundRect(
                color = color,
                topLeft = Offset(size.width * 0.18f, top),
                size = Size(barWidth, barHeight),
                cornerRadius = radius
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width * 0.56f, top),
                size = Size(barWidth, barHeight),
                cornerRadius = radius
            )
        } else {
            val path = Path().apply {
                moveTo(size.width * 0.24f, size.height * 0.14f)
                lineTo(size.width * 0.86f, size.height * 0.5f)
                lineTo(size.width * 0.24f, size.height * 0.86f)
                close()
            }
            drawPath(path = path, color = color)
        }
    }
}

@Composable
private fun SkipNextGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val triangle = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.18f)
            lineTo(size.width * 0.66f, size.height * 0.5f)
            lineTo(size.width * 0.16f, size.height * 0.82f)
            close()
        }
        drawPath(path = triangle, color = color)

        val barWidth = size.width * 0.14f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.72f, size.height * 0.18f),
            size = Size(barWidth, size.height * 0.64f),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}

private fun formatTrackTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

@PreviewLightDark
@Composable
fun SpotifyPlayerCardPreview() {
    VibePulseTheme {
        Surface(
            color = AppTheme.colors.background,
            modifier = Modifier.padding(16.dp)
        ) {
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
    }
}