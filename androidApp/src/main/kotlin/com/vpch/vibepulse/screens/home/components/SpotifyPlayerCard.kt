package com.vpch.vibepulse.screens.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.Track
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.VibePulseTheme
import com.vpch.vibepulse.ui.components.VibePulseButton
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText

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
    val playbackProgress = if (totalDuration > 0L) {
        (positionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val displayedProgress = if (isDragging) dragProgress else playbackProgress
    val displayedPositionMs = if (isDragging) {
        (dragProgress * totalDuration).toLong()
    } else {
        positionMs
    }

    val waveAmplitude by animateFloatAsState(
        targetValue = if (isPlaying && track != null && !isDragging) 1f else 0f,
        label = "waveAmplitude"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
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
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(184.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF101214)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.spotify_logo),
                        contentDescription = null,
                        modifier = Modifier.size(58.dp),
                        tint = Color.White.copy(alpha = 0.25f)
                    )

                    if (!track?.albumArtUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = track.albumArtUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (track != null && track.tempo > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = CircleShape,
                        color = AppTheme.colors.buttonSecondary,
                        border = BorderStroke(1.dp, AppTheme.colors.strokePrimary.copy(alpha = 0.4f))
                    ) {
                        BodyText(
                            text = "${track.tempo} BPM",
                            color = AppTheme.colors.textPrimary,
                            style = AppTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TitleText(
                text = track?.title ?: "Немає активного треку",
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
                text = track?.artist ?: "Увімкніть музику у Spotify",
                color = AppTheme.colors.textSecondary,
                style = AppTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val canSeek = track != null && totalDuration > 0L

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .then(
                            if (canSeek) {
                                Modifier
                                    .pointerInput(totalDuration) {
                                        detectTapGestures { offset ->
                                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            onSeekTo((fraction * totalDuration).toLong())
                                        }
                                    }
                                    .pointerInput(totalDuration) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { offset ->
                                                isDragging = true
                                                dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            },
                                            onHorizontalDrag = { change, _ ->
                                                change.consume()
                                                dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            },
                                            onDragEnd = {
                                                onSeekTo((dragProgress * totalDuration).toLong())
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
                        wavelength = 24.dp,
                        waveSpeed = 16.dp
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
                        text = formatTrackTime(totalDuration),
                        color = AppTheme.colors.textSecondary,
                        style = AppTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalIconButton(
                    onClick = onToggleLike,
                    enabled = track != null,
                    modifier = Modifier.size(44.dp),
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
                    modifier = Modifier.height(48.dp)
                ) {
                    PlayPauseGlyph(
                        isPlaying = isPlaying,
                        color = AppTheme.colors.iconFilled,
                        modifier = Modifier.size(20.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(44.dp),
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