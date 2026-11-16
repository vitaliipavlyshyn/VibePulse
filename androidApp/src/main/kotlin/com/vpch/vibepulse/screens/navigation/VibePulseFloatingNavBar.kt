package com.vpch.vibepulse.screens.navigation

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.VibePulseTheme
import com.vpch.vibepulse.ui.components.text.BodyText

@Composable
fun VibePulseFloatingNavBar(
    currentRoute: Route?,
    onSelectRoute: (Route) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = AppTheme.colors.buttonPrimary,
            border = BorderStroke(
                width = 1.5.dp,
                color = AppTheme.colors.strokePrimary.copy(alpha = 0.55f)
            ),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomBarItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val itemShape = if (isSelected) RoundedCornerShape(22.dp) else CircleShape

                    Row(
                        modifier = Modifier
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                            .height(44.dp)
                            .clip(itemShape)
                            .background(
                                if (isSelected) {
                                    AppTheme.colors.buttonFilled
                                } else {
                                    AppTheme.colors.buttonSecondary
                                }
                            )
                            .clickable { onSelectRoute(item.route) }
                            .padding(horizontal = if (isSelected) 16.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(item.icon),
                            contentDescription = item.contentDescription,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected) {
                                AppTheme.colors.iconFilled
                            } else {
                                AppTheme.colors.iconSecondary
                            }
                        )

                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            BodyText(
                                text = stringResource(item.text),
                                color = AppTheme.colors.onButtonFilled,
                                style = AppTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun VibePulseFloatingNavBarPreview() {
    VibePulseTheme {
        Surface(color = AppTheme.colors.background) {
            VibePulseFloatingNavBar(
                currentRoute = Route.Home,
                onSelectRoute = {}
            )
        }
    }
}