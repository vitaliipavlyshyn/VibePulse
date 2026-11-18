package com.vpch.vibepulse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vpch.vibepulse.R
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.button.VibePulseActionButton
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText

@Composable
internal fun OpenSpotifyTrackDialog(
    title: String,
    artist: String,
    albumArtUrl: String?,
    cadenceOrTempo: Int = 0,
    isTempoBpm: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        OpenSpotifyTrackDialogContent(
            title = title,
            artist = artist,
            albumArtUrl = albumArtUrl,
            cadenceOrTempo = cadenceOrTempo,
            isTempoBpm = isTempoBpm,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun OpenSpotifyTrackDialogContent(
    title: String,
    artist: String,
    albumArtUrl: String?,
    cadenceOrTempo: Int = 0,
    isTempoBpm: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    VibePulseCard(
        shape = RoundedCornerShape(28.dp),
        containerColor = AppTheme.colors.background,
        borderColor = AppTheme.colors.indicator.copy(alpha = 0.45f),
        borderWidth = 1.dp,
        elevation = 8.dp,
        contentPadding = PaddingValues(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VibePulseArtwork(
                imageUrl = albumArtUrl,
                contentDescription = title,
                placeholderIconRes = R.drawable.spotify_logo,
                modifier = Modifier.size(128.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TitleText(
                    text = title,
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val subtitleText = when {
                    cadenceOrTempo > 0 && isTempoBpm ->
                        "$artist · ${stringResource(R.string.player_bpm_badge, cadenceOrTempo)}"
                    cadenceOrTempo > 0 ->
                        stringResource(R.string.stats_details_artist_spm, artist, cadenceOrTempo)
                    else -> artist
                }

                BodyText(
                    text = subtitleText,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TitleText(
                    text = stringResource(R.string.spotify_open_dialog_title),
                    style = AppTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                BodyText(
                    text = stringResource(R.string.spotify_open_dialog_subtitle),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VibePulseActionButton(
                    text = stringResource(R.string.spotify_open_dialog_cancel),
                    onClick = onDismiss,
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )

                VibePulseActionButton(
                    text = stringResource(R.string.spotify_open_dialog_confirm),
                    iconRes = R.drawable.spotify_logo,
                    onClick = onConfirm,
                    isPrimary = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun OpenSpotifyTrackDialogPreview() = VibePulsePreview {
    OpenSpotifyTrackDialogContent(
        title = "Just Keep Watching (From F1 The Album)",
        artist = "Tate McRae",
        albumArtUrl = null,
        cadenceOrTempo = 128,
        isTempoBpm = true,
        onConfirm = {},
        onDismiss = {}
    )
}