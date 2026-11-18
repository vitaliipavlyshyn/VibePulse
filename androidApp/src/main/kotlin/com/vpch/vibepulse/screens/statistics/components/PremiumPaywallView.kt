package com.vpch.vibepulse.screens.statistics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.R
import com.vpch.vibepulse.storage.SubscriptionState
import com.vpch.vibepulse.storage.SubscriptionStatus
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.VibePulseCard
import com.vpch.vibepulse.ui.components.VibePulsePillBadge
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.button.VibePulseActionButton
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText

@Composable
internal fun PremiumPaywallView(
    subscriptionState: SubscriptionState,
    bottomPadding: Dp,
    onActivateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpired = subscriptionState.status == SubscriptionStatus.EXPIRED
    val wasTrial = subscriptionState.isTrial

    val badgeText = stringResource(
        when {
            isExpired && wasTrial -> R.string.paywall_badge_trial_expired
            isExpired -> R.string.paywall_badge_sub_expired
            else -> R.string.paywall_badge_premium
        }
    )

    val titleText = stringResource(
        when {
            isExpired && wasTrial -> R.string.paywall_title_trial_expired
            isExpired -> R.string.paywall_title_sub_expired
            else -> R.string.paywall_title_default
        }
    )

    val descriptionText = if (isExpired) {
        val endDateText = subscriptionState.endEpochMs?.let { formatShortDay(it) }.orEmpty()
        val periodLabel = stringResource(
            if (wasTrial) R.string.paywall_period_label_trial
            else R.string.paywall_period_label_monthly
        )
        stringResource(R.string.paywall_desc_expired, periodLabel, endDateText)
    } else {
        stringResource(R.string.paywall_desc_default)
    }

    val buttonText = stringResource(
        if (isExpired) R.string.paywall_btn_renew else R.string.paywall_btn_try_free
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 16.dp + bottomPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        VibePulseCard(
            shape = RoundedCornerShape(StatisticsDefaults.PaywallCardCornerRadius),
            borderColor = AppTheme.colors.indicator.copy(alpha = 0.5f),
            borderWidth = 1.dp,
            elevation = 0.dp,
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VibePulsePillBadge(
                    text = badgeText,
                    containerColor = AppTheme.colors.indicator.copy(alpha = 0.15f),
                    contentColor = AppTheme.colors.indicator,
                    borderColor = null,
                    fontWeight = FontWeight.ExtraBold
                )

                TitleText(
                    text = titleText,
                    style = AppTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                BodyText(
                    text = descriptionText,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                VibePulseActionButton(
                    text = buttonText,
                    onClick = onActivateClick,
                    isPrimary = true,
                    shape = RoundedCornerShape(StatisticsDefaults.PaywallButtonCornerRadius),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PremiumPaywallViewPreview() = VibePulsePreview {
    PremiumPaywallView(
        subscriptionState = SubscriptionState(status = SubscriptionStatus.NEVER_SUBSCRIBED, isTrial = false),
        bottomPadding = 0.dp,
        onActivateClick = {}
    )
}