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
    val Blue = Color(0xFF6E98FF)
    val BlueDeep = Color(0xFF0E1E4A)
    val Snow = Color(0xFFF4F3FA)
    val Mist = Color(0xFFA9A6BD)
    val Outline = Color(0xFF2E2A44)
    val Success = Color(0xFF6FD3A0)
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
