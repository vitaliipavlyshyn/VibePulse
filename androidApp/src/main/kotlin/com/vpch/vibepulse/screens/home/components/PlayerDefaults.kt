package com.vpch.vibepulse.screens.home.components

import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.R

object PlayerDefaults {
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
