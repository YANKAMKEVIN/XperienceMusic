package com.xperiencemusic.shared.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.ui.brand.BrandWaves
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing

/**
 * An empty screen that still looks finished: an icon in a glowing tile, the brand waves, what will
 * show up here, and the action that fills it when the viewer can take it.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    EmptyStateLayout(title, text, modifier, actionLabel, onAction) {
        Box(
            Modifier
                .size(160.dp)
                .background(
                    Brush.radialGradient(listOf(BrandColors.Indigo.copy(alpha = 0.35f), Color.Transparent)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(28.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = BrandColors.Purple, modifier = Modifier.size(42.dp))
            }
        }
    }
}

/**
 * The days left before an event, as a ring that fills up as it gets closer. [fraction] is how much
 * of the wait is over, 0..1.
 */
@Composable
fun CountdownEmptyState(
    value: String,
    caption: String,
    fraction: Float,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(fraction) { sweep.animateTo(fraction.coerceIn(0.04f, 1f), tween(900, easing = FastOutSlowInEasing)) }
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    EmptyStateLayout(title, text, modifier, actionLabel, onAction, actionIsGradient = false) {
        Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(150.dp)) {
                val stroke = 12.dp.toPx()
                drawArc(track, 0f, 360f, useCenter = false, style = Stroke(stroke))
                drawArc(
                    BrandColors.gradient,
                    startAngle = -90f,
                    sweepAngle = 360f * sweep.value,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = MaterialTheme.typography.headlineLarge)
                Text(caption, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyStateLayout(
    title: String,
    text: String,
    modifier: Modifier,
    actionLabel: String?,
    onAction: () -> Unit,
    actionIsGradient: Boolean = true,
    illustration: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        illustration()
        BrandWaves(Modifier.fillMaxWidth().height(40.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.s, start = Spacing.l, end = Spacing.l),
        )
        if (actionLabel != null) {
            if (actionIsGradient) {
                GradientButton(onClick = onAction, modifier = Modifier.padding(top = Spacing.xl)) {
                    Text(actionLabel, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                TextButton(onClick = onAction, modifier = Modifier.padding(top = Spacing.l)) {
                    Text(actionLabel)
                }
            }
        }
    }
}
