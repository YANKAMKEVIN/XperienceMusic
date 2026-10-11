package com.xperiencemusic.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val colors = darkColorScheme(
    primary = Palette.Violet,
    onPrimary = Palette.Snow,
    primaryContainer = Palette.VioletDeep,
    onPrimaryContainer = Palette.Violet,
    secondary = Palette.Blue,
    onSecondary = Palette.BlueDeep,
    secondaryContainer = Palette.BlueDeep,
    onSecondaryContainer = Palette.Blue,
    background = Palette.Night,
    onBackground = Palette.Snow,
    surface = Palette.Night,
    onSurface = Palette.Snow,
    surfaceVariant = Palette.NightHigh,
    onSurfaceVariant = Palette.Mist,
    surfaceContainer = Palette.NightRaised,
    surfaceContainerHigh = Palette.NightHigh,
    outline = Palette.Outline,
    outlineVariant = Palette.Outline,
    tertiary = Palette.Success,
    error = Palette.Error,
)

/** Dark only: the app is mostly used in dim rehearsal rooms and churches. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
