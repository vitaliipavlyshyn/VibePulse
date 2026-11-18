package com.vpch.vibepulse.screens.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.R
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText

@Composable
internal fun LogoutButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
        color = AppTheme.colors.buttonPrimary,
        border = BorderStroke(1.dp, AppTheme.colors.textError.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            BodyText(
                text = stringResource(R.string.profile_logout_button),
                color = AppTheme.colors.textError,
                style = AppTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
internal fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.buttonPrimary,
        title = {
            TitleText(
                text = stringResource(R.string.profile_logout_dialog_title),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            BodyText(
                text = stringResource(R.string.profile_logout_dialog_message),
                color = AppTheme.colors.textSecondary,
                style = AppTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                BodyText(
                    text = stringResource(R.string.profile_logout_dialog_confirm),
                    color = AppTheme.colors.textError,
                    style = AppTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                BodyText(
                    text = stringResource(R.string.profile_logout_dialog_cancel),
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@PreviewLightDark
@Composable
private fun LogoutButtonPreview() = VibePulsePreview {
    LogoutButton(onClick = {})
}