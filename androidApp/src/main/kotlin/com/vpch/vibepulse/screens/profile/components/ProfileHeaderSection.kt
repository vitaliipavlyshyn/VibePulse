package com.vpch.vibepulse.screens.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vpch.vibepulse.R
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText

@Composable
internal fun ProfileHeaderSection(
    displayName: String?,
    avatarUrl: String?,
    isPremium: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(ProfileDefaults.AvatarSize),
            shape = CircleShape,
            color = AppTheme.colors.buttonSecondary,
            border = BorderStroke(2.dp, AppTheme.colors.indicator.copy(alpha = 0.7f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                TitleText(
                    text = displayName?.firstOrNull()?.uppercase() ?: "V",
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TitleText(
            text = displayName ?: stringResource(R.string.profile_default_name),
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = CircleShape,
            color = if (isPremium) {
                AppTheme.colors.buttonFilled
            } else {
                AppTheme.colors.buttonSecondary
            }
        ) {
            BodyText(
                text = stringResource(
                    if (isPremium) R.string.profile_tier_premium else R.string.profile_tier_free
                ),
                color = if (isPremium) {
                    AppTheme.colors.onButtonFilled
                } else {
                    AppTheme.colors.textSecondary
                },
                style = AppTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}