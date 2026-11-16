package com.vpch.vibepulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.VibePulseTheme

@Composable
fun AppErrorDialog(
    message: String,
    onDismiss: () -> Unit,
    title: String = "Помилка",
    confirmText: String = "Спробувати знову",
    dismissText: String = "Закрити",
    onConfirm: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = AppTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            if (onConfirm != null) {
                Button(
                    onClick = {
                        onDismiss()
                        onConfirm()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(
                        text = confirmText,
                        color = AppTheme.colors.textInverse
                        )
                }
            } else {
                Button(
                    onClick = onDismiss,) {
                    Text(dismissText)
                }
            }
        },
        dismissButton = {
            if (onConfirm != null) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.buttonFilled,
                        contentColor = AppTheme.colors.textInverse
                    )
                ) {
                    Text(dismissText)
                }
            }
        }
    )
}

@PreviewLightDark
@Composable
fun AppErrorDialogPreview() {
    VibePulseTheme {
        Surface(
            modifier = Modifier.size(width = 420.dp, height = 620.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            AppErrorDialog(
                title = "Помилка авторизації",
                message = "Не вдалося підключити акаунт Spotify. Перевірте інтернет або спробуйте ще раз.",
                onDismiss = {},
                onConfirm = {}
            )
        }
    }
}