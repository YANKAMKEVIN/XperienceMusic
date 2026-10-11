package com.xperiencemusic.shared.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Glyphs missing from the core icon set: full screen, text size, slow/fast. */
object StageIcons {
    val Fullscreen: ImageVector by lazy {
        stroked("Fullscreen") {
            moveTo(4f, 9f); verticalLineTo(4f); horizontalLineTo(9f)
            moveTo(15f, 4f); horizontalLineTo(20f); verticalLineTo(9f)
            moveTo(20f, 15f); verticalLineTo(20f); horizontalLineTo(15f)
            moveTo(9f, 20f); horizontalLineTo(4f); verticalLineTo(15f)
        }
    }

    val Minus: ImageVector by lazy { stroked("Minus") { moveTo(5f, 12f); horizontalLineTo(19f) } }

    val Plus: ImageVector by lazy {
        stroked("Plus") {
            moveTo(5f, 12f); horizontalLineTo(19f)
            moveTo(12f, 5f); verticalLineTo(19f)
        }
    }

    private fun stroked(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, pathBuilder = block)
            .build()
}
