package com.xperiencemusic.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val colors = darkColorScheme(
    primary = Palette.Gold,
    onPrimary = Palette.GoldDeep,
    primaryContainer = Palette.GoldDeep,
    onPrimaryContainer = Palette.Gold,
    secondary = Palette.Violet,
    onSecondary = Palette.VioletDeep,
    secondaryContainer = Palette.VioletDeep,
    onSecondaryContainer = Palette.Violet,
    background = Palette.Night,
    onBackground = Palette.Cream,
    surface = Palette.Night,
    onSurface = Palette.Cream,
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
