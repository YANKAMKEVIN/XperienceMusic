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

/**
 * Horizontal tuner: the green band in the middle is "in tune", the needle shows how far the voice
 * is (left = too low, right = too high). [position] is -1..1, null hides the needle.
 */
@Composable
fun TunerNeedle(position: Float?, description: String, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(position ?: 0f, label = "needle")
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val zone = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
    val needle = MaterialTheme.colorScheme.primary
    val center = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = description }) {
        val radius = CornerRadius(12.dp.toPx())
        drawRoundRect(track, cornerRadius = radius)
        // The gauge spans ±100 cents; the band is the ±50 cents that count as in tune.
        val bandWidth = size.width * 0.5f
        drawRoundRect(zone, topLeft = Offset((size.width - bandWidth) / 2, 0f), size = Size(bandWidth, size.height), cornerRadius = radius)
        drawLine(center, Offset(size.width / 2, 8.dp.toPx()), Offset(size.width / 2, size.height - 8.dp.toPx()), strokeWidth = 1.dp.toPx())
        if (position != null) {
            val x = size.width / 2 + animated * (size.width / 2 - 8.dp.toPx())
            drawLine(needle, Offset(x, 4.dp.toPx()), Offset(x, size.height - 4.dp.toPx()), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}
