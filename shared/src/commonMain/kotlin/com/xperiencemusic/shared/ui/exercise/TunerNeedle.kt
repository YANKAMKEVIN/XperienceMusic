package com.xperiencemusic.shared.ui.exercise

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.ui.theme.Radius
import com.xperiencemusic.shared.ui.theme.Sizes

private val GAUGE_HEIGHT = 56.dp
/** How far the graduations stop short of the gauge's edges: the major ones run longer. */
private val MAJOR_TICK_INSET = 6.dp
private val MINOR_TICK_INSET = 16.dp
/** The centre line and the needle stop this far from the edges. */
private val LINE_INSET = 4.dp
/** Keeps the needle's round cap inside the gauge at ±100 cents. */
private val NEEDLE_MARGIN = 8.dp
private val NEEDLE_WIDTH = 6.dp

/**
 * Horizontal tuner: the green band in the middle is "in tune", the needle shows how far the voice
 * is (left = too low, right = too high). [position] is -1..1, null hides the needle.
 */
@Composable
fun TunerNeedle(position: Float?, description: String, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(position ?: 0f, label = "needle")
    val track = MaterialTheme.colorScheme.outline
    val zone = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
    val needle = MaterialTheme.colorScheme.onSurface
    val center = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.fillMaxWidth().height(GAUGE_HEIGHT).semantics { contentDescription = description }) {
        val radius = CornerRadius(Radius.field.toPx())
        // The gauge spans ±100 cents; the band is the ±50 cents that count as in tune.
        val bandWidth = size.width * 0.5f
        drawRoundRect(zone, topLeft = Offset((size.width - bandWidth) / 2, 0f), size = Size(bandWidth, size.height), cornerRadius = radius)
        // Graduations every 12.5 cents, longer every 50.
        for (tick in 0..TICKS) {
            val x = size.width * tick / TICKS
            val isMajor = tick % (TICKS / 4) == 0
            val inset = if (isMajor) MAJOR_TICK_INSET.toPx() else MINOR_TICK_INSET.toPx()
            drawLine(track, Offset(x, inset), Offset(x, size.height - inset), strokeWidth = Sizes.strokeMedium.toPx())
        }
        drawLine(center, Offset(size.width / 2, LINE_INSET.toPx()), Offset(size.width / 2, size.height - LINE_INSET.toPx()), strokeWidth = Sizes.hairline.toPx())
        if (position != null) {
            val x = size.width / 2 + animated * (size.width / 2 - NEEDLE_MARGIN.toPx())
            drawLine(needle, Offset(x, LINE_INSET.toPx()), Offset(x, size.height - LINE_INSET.toPx()), strokeWidth = NEEDLE_WIDTH.toPx(), cap = StrokeCap.Round)
        }
    }
}

private const val TICKS = 16
