package com.xperiencemusic.shared.ui.brand

import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
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
 *
 * With [isActive] false the lines slow down and settle almost flat, then pick up again: in the
 * player they move with the music, so play and pause read at a glance from the whole screen.
 */
@Composable
fun BrandWaves(modifier: Modifier = Modifier, isActive: Boolean = true) {
    val energy by animateFloatAsState(if (isActive) 1f else 0f, tween(SETTLE_MS, easing = LinearEasing), label = "waves-energy")
    var phase by remember { mutableFloatStateOf(STILL_PHASE) }
    val isMoving = energy > 0f && !LocalInspectionMode.current
    val currentEnergy by rememberUpdatedState(energy)
    // Nothing runs while the lines rest: the drift only ticks as long as there is energy left.
    LaunchedEffect(isMoving) {
        if (!isMoving) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            // Whole turns: the lines' speeds are whole numbers, so wrapping at a turn does not jump.
            phase = (phase + (now - last) / NANOS_PER_SECOND * TURN / DRIFT_SECONDS * currentEnergy) % TURN
            last = now
        }
    }
    val swell = REST_SWELL + (1f - REST_SWELL) * energy
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
                    line.amplitude * swell * size.height * (0.35f + 0.65f * envelope)
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

/** A full turn, 2π. */
private const val TURN = 6.2831855f
/** One turn of the slowest line, at full energy. */
private const val DRIFT_SECONDS = 9f
private const val NANOS_PER_SECOND = 1_000_000_000f
/** Where the lines stand in previews, and where they start. */
private const val STILL_PHASE = 0.6f
/** How long the lines take to settle, or to pick up again. */
private const val SETTLE_MS = 900
/** What is left of the lines' height at rest: almost flat, still a wave. */
private const val REST_SWELL = 0.12f

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
