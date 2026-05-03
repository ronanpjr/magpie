package com.magpie.magpie.ui.theme


import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val DarkColorScheme = darkColorScheme(
    primary = MagpiePrimaryDark,
    onPrimary = MagpieOnPrimaryDark,
    primaryContainer = MagpiePrimaryContainerDark,
    onPrimaryContainer = MagpieOnPrimaryContainerDark,
    tertiary = MagpieTertiaryDark,
    onTertiary = MagpieOnTertiaryDark,
    background = MagpieBackgroundDark,
    surface = MagpieSurfaceDark,
    surfaceVariant = MagpieSurfaceVariantDark,
    onBackground = MagpieOnBackgroundDark,
    onSurface = MagpieOnSurfaceDark,
    outline = MagpieOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = MagpiePrimary,
    onPrimary = MagpieOnPrimary,
    primaryContainer = MagpiePrimaryContainer,
    onPrimaryContainer = MagpieOnPrimaryContainer,
    tertiary = MagpieTertiary,
    onTertiary = MagpieOnTertiary,
    background = MagpieBackground,
    surface = MagpieSurface,
    surfaceVariant = MagpieSurfaceVariant,
    onBackground = MagpieOnBackground,
    onSurface = MagpieOnSurface,
    outline = MagpieOutline
)

@Composable
fun MagpieTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MagpieTypography,
        content = content
    )
}