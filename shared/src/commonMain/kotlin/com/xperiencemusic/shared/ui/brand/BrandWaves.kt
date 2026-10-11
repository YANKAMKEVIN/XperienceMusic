package com.xperiencemusic.shared.ui.brand

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import com.xperiencemusic.shared.ui.theme.BrandColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Luminous sound waves: a few thin sine lines, brighter in the middle, drifting slowly.
 * Decorative only; frozen in previews.
 */
@Composable
fun BrandWaves(modifier: Modifier = Modifier) {
    val phase = if (!LocalInspectionMode.current) {
        val transition = rememberInfiniteTransition()
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing), RepeatMode.Restart),
        )
        value
    } else {
        0.6f
    }
    // Offscreen so the edge fade can mask the lines without painting over the background.
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val mid = size.height / 2
        lines.forEachIndexed { index, line ->
            val path = Path()
            val steps = 96
            for (step in 0..steps) {
                val x = size.width * step / steps
                val t = step.toFloat() / steps
                // Amplitude swells towards the centre, like a voice's waveform.
                val envelope = sin(PI * t).toFloat()
                val y = mid + line.offset * size.height +
                    sin(t * line.frequency * 2 * PI + phase * line.speed + index).toFloat() *
                    line.amplitude * size.height * (0.35f + 0.65f * envelope)
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path,
                brush = Brush.horizontalGradient(
                    listOf(BrandColors.Purple, BrandColors.Indigo, BrandColors.Blue).map { it.copy(alpha = line.alpha) },
                ),
                style = Stroke(width = line.width * density),
            )
        }
        // Lines fade out at both edges.
        drawRect(
            Brush.horizontalGradient(
                0f to Color.Transparent, 0.2f to Color.Black, 0.8f to Color.Black, 1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
}

private class WaveLine(
    val offset: Float,
    val amplitude: Float,
    val frequency: Float,
    val speed: Float,
    val width: Float,
    val alpha: Float,
)

private val lines = listOf(
    WaveLine(offset = 0f, amplitude = 0.22f, frequency = 1.6f, speed = 1f, width = 2.2f, alpha = 0.95f),
    WaveLine(offset = 0.02f, amplitude = 0.16f, frequency = 2.1f, speed = -1f, width = 1.4f, alpha = 0.6f),
    WaveLine(offset = -0.03f, amplitude = 0.28f, frequency = 1.2f, speed = 2f, width = 1f, alpha = 0.4f),
    WaveLine(offset = 0.05f, amplitude = 0.10f, frequency = 3f, speed = -2f, width = 0.8f, alpha = 0.3f),
)
