package com.xperiencemusic.shared.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import com.xperiencemusic.shared.ui.theme.Sizes

/** Material's transport glyphs; the core icon set only ships "play". */
internal object PlayerIcons {
    val SkipPrevious: ImageVector by lazy {
        filled("SkipPrevious") {
            moveTo(6f, 6f); horizontalLineToRelative(2f); verticalLineToRelative(12f); horizontalLineTo(6f); close()
            moveTo(9.5f, 12f); lineToRelative(8.5f, 6f); verticalLineTo(6f); close()
        }
    }

    val SkipNext: ImageVector by lazy {
        filled("SkipNext") {
            moveTo(6f, 18f); lineToRelative(8.5f, -6f); lineTo(6f, 6f); close()
            moveTo(16f, 6f); verticalLineToRelative(12f); horizontalLineToRelative(2f); verticalLineTo(6f); close()
        }
    }

    private fun filled(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = Sizes.icon, defaultHeight = Sizes.icon, viewportWidth = 24f, viewportHeight = 24f)
            .path(fill = SolidColor(Color.Black), pathBuilder = block)
            .build()
}
