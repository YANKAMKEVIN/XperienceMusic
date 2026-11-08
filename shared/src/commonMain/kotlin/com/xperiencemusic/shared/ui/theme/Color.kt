package com.xperiencemusic.shared.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Xperience Music art direction: deep night, violet to blue, near-white text. */
internal object Palette {
    val Night = Color(0xFF0B0A14)
    val NightRaised = Color(0xFF15131F)
    val NightHigh = Color(0xFF221F32)
    val Violet = Color(0xFF9B7BFF)
    val VioletDeep = Color(0xFF24184F)
    /** A chosen chip or a tonal button: solid enough for white text (about 6:1). */
    val VioletSolid = Color(0xFF6A4FE0)
    val Blue = Color(0xFF6E98FF)
    val BlueDeep = Color(0xFF0E1E4A)
    val Snow = Color(0xFFF4F3FA)
    val Mist = Color(0xFFA9A6BD)
    /** Body text on a raised gradient card: a step brighter than [Mist]. */
    val MistLight = Color(0xFFC9C6DA)
    /** Eyebrow text over the violet gradient. */
    val VioletLight = Color(0xFFB9A2FF)
    /** The hairline around a violet gradient card. */
    val VioletEdge = Color(0xFF3A3260)
    val Outline = Color(0xFF2E2A44)
    val Success = Color(0xFF6FD3A0)
    /** A black piano key, and the ink of the labels on the white ones. */
    val Ebony = Color(0xFF161320)
    /** The line between two piano keys. */
    val KeyEdge = Color(0xFF555164)
    val Error = Color(0xFFFF8A8A)
}

/** The brand gradient of the logo, wordmark and main buttons. */
object BrandColors {
    val Purple = Color(0xFFB46CFF)
    val Indigo = Color(0xFF6D5BFF)
    val Blue = Color(0xFF3D7BFF)

    /** Something missing, gently: a part nobody covers yet. */
    val Amber = Color(0xFFF2B35B)

    val gradient: Brush get() = Brush.linearGradient(listOf(Purple, Indigo, Blue))
    val horizontalGradient: Brush get() = Brush.horizontalGradient(listOf(Purple, Indigo, Blue))
}
