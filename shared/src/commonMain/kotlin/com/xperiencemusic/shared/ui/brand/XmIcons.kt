package com.xperiencemusic.shared.ui.brand

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Line icons of the tab bar, drawn like the brand mockups (2 dp strokes, round ends). */
object XmIcons {
    /** Two beamed eighth notes: the week's songs. */
    val Notes: ImageVector by lazy {
        ImageVector.Builder("Notes", 24.dp, 24.dp, 24f, 24f)
            .path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 18f); verticalLineTo(5.5f); lineTo(19f, 3.5f); verticalLineTo(16f)
            }
            .path(fill = SolidColor(Color.Black)) {
                // Two tilted heads, approximated by circles.
                moveTo(9.5f, 18f); arcToRelative(3f, 2.4f, 0f, true, true, -6f, 0f); arcToRelative(3f, 2.4f, 0f, true, true, 6f, 0f); close()
                moveTo(19.5f, 16f); arcToRelative(3f, 2.4f, 0f, true, true, -6f, 0f); arcToRelative(3f, 2.4f, 0f, true, true, 6f, 0f); close()
            }
            .build()
    }

    /** An open book: the library. */
    val Book: ImageVector by lazy {
        icon("Book") {
            moveTo(12f, 6.5f)
            curveTo(10f, 5f, 7f, 4.5f, 3.5f, 5f); verticalLineTo(18.5f)
            curveTo(7f, 18f, 10f, 18.5f, 12f, 20f)
            curveTo(14f, 18.5f, 17f, 18f, 20.5f, 18.5f); verticalLineTo(5f)
            curveTo(17f, 4.5f, 14f, 5f, 12f, 6.5f); close()
            moveTo(12f, 6.5f); verticalLineTo(20f)
        }
    }

    /** Rising bars: progress. */
    val Bars: ImageVector by lazy {
        icon("Bars") {
            moveTo(6f, 19f); verticalLineTo(14f)
            moveTo(12f, 19f); verticalLineTo(9f)
            moveTo(18f, 19f); verticalLineTo(4.5f)
        }
    }

    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()
}
