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

    /** A stage microphone: singers. */
    val Mic: ImageVector by lazy {
        icon("Mic") {
            moveTo(9f, 6f); arcToRelative(3f, 3f, 0f, false, true, 6f, 0f); verticalLineTo(11f)
            arcToRelative(3f, 3f, 0f, false, true, -6f, 0f); close()
            moveTo(5.5f, 11f); arcToRelative(6.5f, 6.5f, 0f, false, false, 13f, 0f)
            moveTo(12f, 17.5f); verticalLineTo(21f)
            moveTo(9f, 21f); horizontalLineTo(15f)
        }
    }

    /** A keyboard seen from above. */
    val Piano: ImageVector by lazy {
        icon("Piano") {
            moveTo(3.5f, 5f); horizontalLineTo(20.5f); verticalLineTo(19f); horizontalLineTo(3.5f); close()
            moveTo(9f, 19f); verticalLineTo(13f)
            moveTo(15f, 19f); verticalLineTo(13f)
            moveTo(7.5f, 5f); verticalLineTo(13f); horizontalLineTo(10.5f); verticalLineTo(5f)
            moveTo(13.5f, 5f); verticalLineTo(13f); horizontalLineTo(16.5f); verticalLineTo(5f)
        }
    }

    /** A bass guitar: round body, long neck. */
    val Bass: ImageVector by lazy {
        icon("Bass") {
            moveTo(13.5f, 10.5f); lineTo(20f, 4f)
            moveTo(18.5f, 2.5f); lineTo(21.5f, 5.5f)
            moveTo(12.6f, 11.4f)
            arcToRelative(5f, 5f, 0f, true, true, -1.5f, -1.5f)
            moveTo(10.5f, 15f); arcToRelative(1.5f, 1.5f, 0f, true, true, -3f, 0f); arcToRelative(1.5f, 1.5f, 0f, true, true, 3f, 0f)
        }
    }

    /** A snare drum and its sticks. */
    val Drums: ImageVector by lazy {
        icon("Drums") {
            moveTo(4f, 11f); arcToRelative(8f, 3f, 0f, true, false, 16f, 0f); arcToRelative(8f, 3f, 0f, true, false, -16f, 0f)
            moveTo(4f, 11f); verticalLineTo(17f); arcToRelative(8f, 3f, 0f, false, false, 16f, 0f); verticalLineTo(11f)
            moveTo(6f, 3f); lineTo(11f, 8.5f)
            moveTo(18f, 3f); lineTo(13f, 8.5f)
        }
    }

    /** A person: the name field. */
    val Person: ImageVector by lazy {
        icon("Person") {
            moveTo(15.5f, 8f); arcToRelative(3.5f, 3.5f, 0f, true, true, -7f, 0f); arcToRelative(3.5f, 3.5f, 0f, true, true, 7f, 0f)
            moveTo(5f, 20f); curveTo(5.5f, 16f, 8.5f, 14f, 12f, 14f); curveTo(15.5f, 14f, 18.5f, 16f, 19f, 20f)
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
