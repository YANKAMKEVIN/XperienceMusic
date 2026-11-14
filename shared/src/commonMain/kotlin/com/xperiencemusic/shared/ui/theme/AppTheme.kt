package com.xperiencemusic.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

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

/**
 * Text fields, menus and snack bars draw from extraSmall: rounded to sit with the cards. The
 * other slots hold the app's radii, which equal Material's defaults for medium, large and
 * extraLarge; small keeps Material's 8 dp (the filter chips draw from it).
 */
private val shapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.field),
    medium = RoundedCornerShape(Radius.field),
    large = RoundedCornerShape(Radius.card),
    extraLarge = RoundedCornerShape(Radius.hero),
)

private val LocalAppColors = staticCompositionLocalOf { darkAppColors }

/** Dark only: the app is mostly used in dim rehearsal rooms and churches. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides darkAppColors) {
        MaterialTheme(colorScheme = colors, shapes = shapes, content = content)
    }
}

/** What the app's theme adds to [MaterialTheme]. */
object AppTheme {
    /** Status and card colours Material's scheme has no slot for. */
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}
