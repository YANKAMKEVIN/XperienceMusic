package com.xperiencemusic.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * The text styles Material's scale has no slot for. Material's own `Typography` stays the
 * default one; these are built on `bodyLarge` (the style Material provides to plain text) so
 * that they read exactly like the `fontSize` and `fontWeight` they replace.
 */
object AppTypography {
    /** A large digit: the boxes of the sign-in code. */
    val displayNumber: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.SemiBold)

    /** The "/7" after a big count: smaller than the count, still bold. */
    val numberSuffix: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)

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
