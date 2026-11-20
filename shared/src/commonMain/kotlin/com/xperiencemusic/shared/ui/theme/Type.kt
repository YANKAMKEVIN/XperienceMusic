package com.xperiencemusic.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.outfit_black
import com.xperiencemusic.shared.resources.outfit_bold
import com.xperiencemusic.shared.resources.outfit_medium
import com.xperiencemusic.shared.resources.outfit_semibold
import org.jetbrains.compose.resources.Font
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * The brand's title face: Outfit, a geometric sans-serif that sits with the XM monogram and gives
 * titles the same character on Android and iPhone. Under the SIL Open Font License, shipped with
 * the app (`composeResources/files/outfit-OFL.txt`). Only the weights the titles use are bundled;
 * a lighter weight asked of it draws in Medium.
 */
@Composable
internal fun brandFontFamily(): FontFamily = FontFamily(
    Font(Res.font.outfit_medium, FontWeight.Medium),
    Font(Res.font.outfit_semibold, FontWeight.SemiBold),
    Font(Res.font.outfit_bold, FontWeight.Bold),
    Font(Res.font.outfit_black, FontWeight.Black),
)

/**
 * Material's scale with [titles] on everything that is a title: display, headline and title.
 * Body and label keep the system font: lyrics, chords and running text stay as plain to read as
 * they were.
 */
internal fun appTypography(titles: FontFamily): Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = titles),
        displayMedium = displayMedium.copy(fontFamily = titles),
        displaySmall = displaySmall.copy(fontFamily = titles),
        headlineLarge = headlineLarge.copy(fontFamily = titles),
        headlineMedium = headlineMedium.copy(fontFamily = titles),
        headlineSmall = headlineSmall.copy(fontFamily = titles),
        titleLarge = titleLarge.copy(fontFamily = titles),
        titleMedium = titleMedium.copy(fontFamily = titles),
        titleSmall = titleSmall.copy(fontFamily = titles),
    )
}

/**
 * The text styles Material's scale has no slot for. They are built on `bodyLarge` (the style
 * Material provides to plain text) so that they read exactly like the `fontSize` and `fontWeight`
 * they replace; the large numbers take the title face.
 */
object AppTypography {
    /** A large digit: the boxes of the sign-in code. */
    val displayNumber: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTheme.titleFont)

    /** The "/7" after a big count: smaller than the count, still bold. */
    val numberSuffix: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = AppTheme.titleFont)

    /** The label of a segmented tab. */
    val tabLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)

    /**
     * Typed text of the large pill field (the e-mail of the sign-in). A raw style, as a
     * `BasicTextField` does not inherit Material's text style; its placeholder uses [fieldPlaceholder].
     */
    val fieldInput = TextStyle(fontSize = 17.sp)

    /** The placeholder of the large pill field, at the size of [fieldInput]. */
    val fieldPlaceholder: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyLarge.merge(fieldInput)
}

/** The sizes of the « XPERIENCE MUSIC » wordmark, from the sign-in header to the splash. */
enum class WordmarkSize(val fontSize: TextUnit) {
    /** The sign-in screen once the keyboard is up. */
    Small(22.sp),
    /** The brand header above the sign-in and the onboarding. */
    Medium(26.sp),
    Large(30.sp),
    /** The splash screen. */
    Splash(34.sp),
}
