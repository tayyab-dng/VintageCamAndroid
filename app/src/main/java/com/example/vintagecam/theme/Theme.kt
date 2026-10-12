package com.example.vintagecam.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VintageColorScheme = darkColorScheme(
    primary = VintageAmber,
    onPrimary = Color.Black,
    primaryContainer = VintageGold,
    onPrimaryContainer = Color.Black,
    secondary = VintageFilmYellow,
    onSecondary = Color.Black,
    secondaryContainer = VintageLeatherGrip,
    onSecondaryContainer = VintageTextLight,
    tertiary = VintageWarmOrange,
    onTertiary = Color.Black,
    background = VintageBlack,
    onBackground = VintageTextLight,
    surface = VintageBodyDark,
    onSurface = VintageTextLight,
    surfaceVariant = VintageSurfaceElevated,
    onSurfaceVariant = VintageTextMuted,
    outline = VintageMetalDark
)

@Composable
fun VintageCamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VintageColorScheme,
        typography = Typography,
        content = content
    )
}
