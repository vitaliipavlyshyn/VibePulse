package com.vpch.vibepulse.screens.profile.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.UserTier
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.VibePulseArtwork
import com.vpch.vibepulse.ui.components.VibePulsePillBadge
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.text.TitleText

@Composable
internal fun ProfileHeaderSection(
    displayName: String?,
    avatarUrl: String?,
    tier: UserTier
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VibePulseArtwork(
            imageUrl = avatarUrl,
            contentDescription = null,
            shape = CircleShape,
            placeholderText = displayName?.firstOrNull()?.uppercase() ?: "V",
            modifier = Modifier
                .size(ProfileDefaults.AvatarSize)
                .border(2.dp, AppTheme.colors.indicator.copy(alpha = 0.7f), CircleShape)
        )

        Spacer(modifier = Modifier.height(12.dp))

        TitleText(
            text = displayName ?: stringResource(R.string.unknown),
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        val hasAccess = tier.hasPremiumAccess
        VibePulsePillBadge(
            text = stringResource(
                when (tier) {
                    UserTier.FREE -> R.string.profile_tier_free
                    UserTier.TRIAL -> R.string.profile_tier_trial
                    UserTier.PREMIUM -> R.string.profile_tier_premium
                }
            ),
            containerColor = if (hasAccess) AppTheme.colors.buttonFilled else AppTheme.colors.buttonSecondary,
            contentColor = if (hasAccess) AppTheme.colors.onButtonFilled else AppTheme.colors.textSecondary,
            borderColor = null,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@PreviewLightDark
@Composable
private fun ProfileHeaderSectionPreview() = VibePulsePreview {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProfileHeaderSection(displayName = "Віталій", avatarUrl = null, tier = UserTier.PREMIUM)
        ProfileHeaderSection(displayName = "Vitalii", avatarUrl = null, tier = UserTier.FREE)
    }
}