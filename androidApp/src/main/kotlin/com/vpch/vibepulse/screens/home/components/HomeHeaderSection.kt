package com.vpch.vibepulse.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.User
import com.vpch.vibepulse.domain.model.WeatherSnapshot
import com.vpch.vibepulse.ui.AppTheme
import com.vpch.vibepulse.ui.components.VibePulseArtwork
import com.vpch.vibepulse.ui.components.VibePulsePreview
import com.vpch.vibepulse.ui.components.text.BodyText
import com.vpch.vibepulse.ui.components.text.TitleText
import kotlin.math.roundToInt

@Composable
internal fun HomeHeaderSection(
    user: User?,
    weather: WeatherSnapshot?,
    isWeatherLoading: Boolean,
    hasLocationPermission: Boolean,
    isLocationEnabled: Boolean,
    onRefreshWeather: () -> Unit,
    onRequestLocation: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(HomeDefaults.HeaderProfileCornerRadius))
                .clickable(onClick = onProfileClick)
                .padding(vertical = 4.dp)
        ) {
            VibePulseArtwork(
                imageUrl = user?.avatarUrl,
                contentDescription = null,
                shape = CircleShape,
                placeholderText = user?.displayName?.firstOrNull()?.uppercase() ?: "V",
                modifier = Modifier
                    .size(HomeDefaults.AvatarSize)
                    .border(1.dp, AppTheme.colors.strokePrimary.copy(alpha = 0.5f), CircleShape)
            )

            Spacer(modifier = Modifier.width(HomeDefaults.SectionSpacing))

            Column {
                TitleText(
                    text = user?.displayName ?: stringResource(R.string.unknown),
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BodyText(
                    text = stringResource(
                        if (user?.hasPremium == true) R.string.spotify_premium else R.string.spotify_not_connected
                    ),
                    color = AppTheme.colors.textSecondary,
                    style = AppTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        WeatherBadge(
            weather = weather,
            isLoading = isWeatherLoading,
            hasPermission = hasLocationPermission,
            isLocationEnabled = isLocationEnabled,
            onClick = { if (!hasLocationPermission) onRequestLocation() else onRefreshWeather() }
        )
    }
}

@Composable
private fun WeatherBadge(
    weather: WeatherSnapshot?,
    isLoading: Boolean,
    hasPermission: Boolean,
    isLocationEnabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = AppTheme.colors.buttonPrimary,
        border = BorderStroke(1.dp, AppTheme.colors.strokePrimary.copy(alpha = 0.5f)),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when {
                isLoading -> {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(HomeDefaults.WeatherLoaderSize),
                        color = AppTheme.colors.indicator,
                        wavelength = HomeDefaults.WeatherLoaderWavelength
                    )
                    BodyText(
                        text = stringResource(R.string.weather),
                        color = AppTheme.colors.textSecondary,
                        style = AppTheme.typography.labelMedium
                    )
                }

                weather != null -> {
                    if (!weather.iconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = weather.iconUrl,
                            contentDescription = weather.condition,
                            colorFilter = ColorFilter.tint(AppTheme.colors.indicator),
                            modifier = Modifier.size(HomeDefaults.WeatherIconSize)
                        )
                    } else {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.location_on),
                            contentDescription = null,
                            modifier = Modifier.size(HomeDefaults.LocationIconSize),
                            tint = AppTheme.colors.indicator
                        )
                    }
                    Column {
                        BodyText(
                            text = stringResource(
                                R.string.home_weather_format,
                                weather.temperature.roundToInt(),
                                weather.condition
                            ),
                            color = AppTheme.colors.textPrimary,
                            style = AppTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (weather.area.isNotBlank()) {
                            BodyText(
                                text = weather.area,
                                color = AppTheme.colors.textSecondary,
                                style = AppTheme.typography.labelSmall
                            )
                        }
                    }
                }

                else -> {
                    val (labelRes, tintColor) = when {
                        !hasPermission -> R.string.allow_location to AppTheme.colors.iconSecondary
                        !isLocationEnabled -> R.string.turn_on_gps to AppTheme.colors.textError
                        else -> R.string.update_weather to AppTheme.colors.iconSecondary
                    }
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.location_on),
                        contentDescription = null,
                        modifier = Modifier.size(HomeDefaults.LocationIconSize),
                        tint = tintColor
                    )
                    BodyText(
                        text = stringResource(labelRes),
                        color = if (!isLocationEnabled && hasPermission) AppTheme.colors.textError else AppTheme.colors.textPrimary,
                        style = AppTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HomeHeaderSectionPreview() = VibePulsePreview {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HomeHeaderSection(
            user = User(id = "1", displayName = "Vitalii", hasPremium = true),
            weather = WeatherSnapshot(temperature = 14.4, condition = "Ясно", area = "Львів"),
            isWeatherLoading = false,
            hasLocationPermission = true,
            isLocationEnabled = true,
            onRefreshWeather = {},
            onRequestLocation = {}
        )
        HomeHeaderSection(
            user = null,
            weather = null,
            isWeatherLoading = false,
            hasLocationPermission = false,
            isLocationEnabled = false,
            onRefreshWeather = {},
            onRequestLocation = {}
        )
    }
}