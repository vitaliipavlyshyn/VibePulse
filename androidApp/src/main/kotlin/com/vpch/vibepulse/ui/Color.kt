package com.vpch.vibepulse.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color


val GoldPrimary = Color(0xFFC89F65)
val GoldLight = Color(0xFFDFBA7D)
val GoldDark = Color(0xFFB0803F)

val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF4F4F5)
val LightSurfaceSecondary = Color(0xFFE4E4E7)
val LightOnBackground = Color(0xFF1C1B1F)
val LightOnBackgroundMuted = Color(0xFF6E6D72)
val LightOnPrimary = Color(0xFFFFFFFF)

val SilverMetallic = Color(0xFFC2CBD6)
val SilverSheen = Color(0xFFDCE3EC)
val SilverSteel = Color(0xFF7E8896)
val SilverTrack = Color(0xFF3A424E)

val DarkBackground = Color(0xFF0D0F13)
val DarkSurface = Color(0xFF161920)
val DarkSurfaceSecondary = Color(0xFF212630)
val DarkOnBackground = Color(0xFFEAEEF5)
val DarkTextMuted = Color(0xFF919AA8)
val DarkOnPrimary = Color(0xFF0D0F13)

val ErrorRed = Color(0xFFE04848)

@Immutable
data class AppColors(
    val backgroundGradient: List<Color>,
    val background: Color,
    val buttonPrimary: Color,
    val buttonSecondary: Color,
    val buttonFilled: Color,
    val onButtonFilled: Color,
    val strokePrimary: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    val iconFilled: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textInverse: Color,
    val textError: Color,
    val indicator: Color,
    val trackIndicator: Color,
    val title: Color,
    val errorContainer: Color
)

val DarkPalette = AppColors(
    backgroundGradient = listOf(Color(0xFF090B0E), DarkBackground, DarkSurface),
    background = DarkBackground,
    buttonPrimary = DarkSurface,
    buttonSecondary = DarkSurfaceSecondary,
    buttonFilled = SilverMetallic,
    onButtonFilled = DarkOnPrimary,
    strokePrimary = SilverSteel,
    iconPrimary = DarkOnBackground,
    iconSecondary = DarkTextMuted,
    iconFilled = DarkOnPrimary,
    textPrimary = DarkOnBackground,
    textSecondary = DarkTextMuted,
    textInverse = DarkOnPrimary,
    textError = ErrorRed,
    indicator = SilverMetallic,
    trackIndicator = SilverTrack,
    title = SilverSheen,
    errorContainer = ErrorRed
)

val LightPalette = AppColors(
    backgroundGradient = listOf(LightBackground, Color(0xFFF9F9FA), LightSurface),
    background = LightBackground,
    buttonPrimary = LightSurface,
    buttonSecondary = LightSurfaceSecondary,
    buttonFilled = GoldPrimary,
    onButtonFilled = LightOnPrimary,
    strokePrimary = Color(0xFFA1A1A6),
    iconPrimary = LightOnBackground,
    iconSecondary = LightOnBackgroundMuted,
    iconFilled = LightOnPrimary,
    textPrimary = LightOnBackground,
    textSecondary = LightOnBackgroundMuted,
    textInverse = LightOnPrimary,
    textError = ErrorRed,
    indicator = GoldPrimary,
    trackIndicator = GoldLight,
    title = GoldLight,
    errorContainer = ErrorRed
)