package com.xperiencemusic.shared.ui.progress

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.xperiencemusic.shared.domain.model.StepState
import com.xperiencemusic.shared.presentation.progress.StepUi
import com.xperiencemusic.shared.presentation.progress.TierUi
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing

/** Horizontal positions the path snakes through, as fractions of the width. */
private val lanes = listOf(0.5f, 0.74f, 0.5f, 0.26f)
private val rowHeight = 104.dp
private val nodeSize = 60.dp

/**
 * One tier of the path: its title, then its steps on a winding line. Done steps are filled with
 * the brand gradient, the open one pulses, the rest are locked.
 */
@Composable
fun TierPath(
    tier: TierUi,
    title: String,
    firstLane: Int,
    labelOpen: String,
    onStepClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s),
        ) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.14.em),
                color = if (tier.isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (tier.isComplete) {
                Icon(XmIcons.Medal, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(rowHeight * tier.steps.size)) {
            val width = maxWidth
            val doneColor = BrandColors.Indigo
            val lockedColor = MaterialTheme.colorScheme.outline
            fun center(index: Int): Pair<Dp, Dp> =
                width * lanes[(firstLane + index) % lanes.size] to rowHeight * index + rowHeight / 2
            Canvas(Modifier.fillMaxWidth().height(rowHeight * tier.steps.size)) {
                for (i in 0 until tier.steps.lastIndex) {
                    val (x1, y1) = center(i)
                    val (x2, y2) = center(i + 1)
                    val start = Offset(x1.toPx(), y1.toPx())
                    val end = Offset(x2.toPx(), y2.toPx())
                    val path = Path().apply {
                        moveTo(start.x, start.y)
                        cubicTo(start.x, (start.y + end.y) / 2, end.x, (start.y + end.y) / 2, end.x, end.y)
                    }
                    val done = tier.steps[i + 1].state != StepState.LOCKED
                    drawPath(
                        path,
                        color = if (done) doneColor else lockedColor,
                        style = Stroke(
                            width = 5.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = if (done) null else PathEffect.dashPathEffect(floatArrayOf(2f, 14.dp.toPx())),
                        ),
                    )
                }
            }
            tier.steps.forEachIndexed { index, step ->
                val (x, y) = center(index)
                val isRight = x > width / 2
                StepNode(
                    step,
                    onClick = { onStepClick(step.id) },
                    modifier = Modifier.offset(x - nodeSize / 2, y - nodeSize / 2),
                )
                StepLabel(
                    step,
                    labelOpen = labelOpen,
                    alignEnd = isRight,
                    modifier = Modifier
                        .width(width / 2 - nodeSize / 2 - Spacing.m)
                        .offset(
                            x = if (isRight) x - nodeSize / 2 - Spacing.m - (width / 2 - nodeSize / 2 - Spacing.m) else x + nodeSize / 2 + Spacing.m,
                            y = y - 20.dp,
                        ),
                )
            }
        }
    }
}

@Composable
private fun StepNode(step: StepUi, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val pulse = if (step.state == StepState.CURRENT) {
        val transition = rememberInfiniteTransition()
        val value by transition.animateFloat(1f, 1.18f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse))
        value
    } else {
        1f
    }
    Box(modifier.size(nodeSize), contentAlignment = Alignment.Center) {
        if (step.state == StepState.CURRENT) {
            // The open step glows and breathes.
            Box(
                Modifier
                    .size(nodeSize)
                    .scale(pulse * 1.25f)
                    .background(Brush.radialGradient(listOf(BrandColors.Indigo.copy(alpha = 0.45f), Color.Transparent)), CircleShape),
            )
        }
        Box(
            Modifier
                .size(nodeSize)
                .clip(CircleShape)
                .background(
                    when (step.state) {
                        StepState.DONE -> BrandColors.gradient
                        StepState.CURRENT -> SolidColor(colors.background)
                        StepState.LOCKED -> SolidColor(colors.surfaceContainer)
                    },
                )
                .border(
                    width = if (step.state == StepState.CURRENT) 3.dp else 1.dp,
                    color = when (step.state) {
                        StepState.CURRENT -> colors.primary
                        StepState.LOCKED -> colors.outlineVariant
                        StepState.DONE -> Color.Transparent
                    },
                    shape = CircleShape,
                )
                .clickable(enabled = step.state != StepState.LOCKED, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = step.title },
            contentAlignment = Alignment.Center,
        ) {
            val (icon, tint) = when {
                step.state == StepState.DONE -> Icons.Filled.Check to Color.White
                step.state == StepState.CURRENT -> Icons.Filled.PlayArrow to colors.onSurface
                step.isChallenge -> XmIcons.Trophy to colors.outline
                else -> XmIcons.Lock to colors.outline
            }
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun StepLabel(step: StepUi, labelOpen: String, alignEnd: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val isLocked = step.state == StepState.LOCKED
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        if (step.state == StepState.CURRENT) {
            Text(
                labelOpen,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrandColors.horizontalGradient)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Text(
            step.title,
            style = MaterialTheme.typography.titleSmall,
            color = if (isLocked) colors.onSurfaceVariant.copy(alpha = 0.6f) else colors.onSurface,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (step.detail.isNotEmpty()) {
            Text(
                step.detail,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant.copy(alpha = if (isLocked) 0.5f else 1f),
            )
        }
    }
}
