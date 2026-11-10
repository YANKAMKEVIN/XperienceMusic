package com.xperiencemusic.shared.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import com.xperiencemusic.shared.ui.theme.Sizes

/** The material "pause" glyph; the core icon set ships play but not pause. */
val PauseIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Pause", defaultWidth = Sizes.icon, defaultHeight = Sizes.icon, viewportWidth = 24f, viewportHeight = 24f)
        .path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 19f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineTo(6f); close()
            moveTo(14f, 5f); verticalLineToRelative(14f); horizontalLineToRelative(4f); verticalLineTo(5f); close()
        }
        .build()
}
