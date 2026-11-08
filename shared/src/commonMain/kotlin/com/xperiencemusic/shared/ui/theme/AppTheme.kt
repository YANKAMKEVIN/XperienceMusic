package com.xperiencemusic.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

private val colors = darkColorScheme(
    primary = Palette.Violet,
    onPrimary = Palette.Snow,
    primaryContainer = Palette.VioletDeep,
    onPrimaryContainer = Palette.Violet,
    secondary = Palette.Blue,
    onSecondary = Palette.BlueDeep,
    // Selected chips and tonal buttons (▶ of the setlist) fill with solid violet and white content:
    // blue on night blue was barely visible.
    secondaryContainer = Palette.VioletSolid,
    onSecondaryContainer = Palette.Snow,
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

/**
 * Switches that stay visible when off: a light border and a light grey thumb on the dark track,
 * where Material's defaults drew a dark track on a dark background.
 */
@Composable
fun appSwitchColors(): SwitchColors = SwitchDefaults.colors(
    uncheckedThumbColor = Palette.Mist,
    uncheckedTrackColor = Palette.NightHigh,
    uncheckedBorderColor = Palette.Mist,
)

/** Text fields, menus and snack bars draw from extraSmall: rounded to sit with the cards. */
private val shapes = Shapes(extraSmall = RoundedCornerShape(Radius.field))

/** Dark only: the app is mostly used in dim rehearsal rooms and churches. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, shapes = shapes, content = content)
}
