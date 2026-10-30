package com.xperiencemusic.shared.ui.progress

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Fades and lifts an element in once, [order] steps after the screen opened. */
@Composable
internal fun Modifier.enter(order: Int): Modifier {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(order * 70L)
        shown.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        alpha = shown.value
        translationY = (1 - shown.value) * 24.dp.toPx()
    }
}

/** How one objective colours its segment of the level ring. */
enum class RingSegment { VALIDATED, WAITING, REWORK, STARTED, TODO }

/**
 * The way to the next level as a ring of one segment per objective. Segments sweep in one after
 * the other; an objective under way glows in the brand gradient, and a small dot orbits. With
 * [calm] ("Réduire les animations") nothing moves: the segments are drawn whole and the dot rests.
 */
@Composable
internal fun LevelRing(segments: List<RingSegment>, description: String, modifier: Modifier = Modifier, calm: Boolean = false, content: @Composable () -> Unit) {
    val sweeps = remember(segments.size) { List(segments.size) { Animatable(0f) } }
    LaunchedEffect(segments.size, calm) {
        sweeps.forEachIndexed { i, sweep ->
            launch {
                if (calm) return@launch sweep.snapTo(1f)
                delay(200L + i * 90L)
                sweep.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
            }
        }
    }
    var orbit = 0f
    var glow = 1f
    if (!calm) {
        val motion = rememberInfiniteTransition(label = "ring")
        orbit = motion.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "orbit").value
        glow = motion.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1_400), RepeatMode.Reverse), label = "glow").value
    }
    Box(modifier.size(200.dp).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(200.dp)) {
            val stroke = 12.dp.toPx()
            val radius = size.minDimension / 2 - 22.dp.toPx()
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            val count = segments.size.coerceAtLeast(1)
            val slot = 360f / count
            val gap = if (count == 1) 0f else 10f
            segments.forEachIndexed { i, segment ->
                val start = -90f + i * slot + gap / 2
                val sweep = slot - gap
                drawArc(Palette.NightHigh, start, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                val drawn = sweep * sweeps[i].value
                if (drawn <= 0f) return@forEachIndexed
                when (segment) {
                    // An untouched objective stays plain: at 0/7 the ring must not look under way.
                    RingSegment.TODO -> Unit
                    RingSegment.STARTED ->
                        drawArc(BrandColors.gradient, start, drawn, false, topLeft, arcSize, alpha = glow, style = Stroke(stroke, cap = StrokeCap.Round))
                    else ->
                        drawArc(segment.color(), start, drawn, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            val angle = (orbit - 90f) * PI.toFloat() / 180f
            val orbitRadius = radius + 16.dp.toPx()
            drawCircle(Palette.Blue, 3.dp.toPx(), Offset(center.x + orbitRadius * cos(angle), center.y + orbitRadius * sin(angle)))
        }
        content()
    }
}

private fun RingSegment.color(): Color = when (this) {
    RingSegment.VALIDATED -> Palette.Success
    RingSegment.WAITING -> Palette.Blue
    RingSegment.REWORK -> BrandColors.Amber
    RingSegment.STARTED -> Palette.Violet
    RingSegment.TODO -> Palette.NightHigh
}

/** A number that counts up from zero the first time it shows. */
@Composable
internal fun CountUp(target: Int, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(target) {
        delay(450)
        while (shown != target) {
            shown += if (target > shown) 1 else -1
            delay(90)
        }
    }
    Text("$shown", style = style, modifier = modifier)
}

/** A small equalizer that dances while the focus card waits for a tap. */
@Composable
internal fun Equalizer(modifier: Modifier = Modifier, color: Color = Palette.Violet) {
    val time by rememberInfiniteTransition(label = "eq").animateFloat(0f, 1f, infiniteRepeatable(tween(1_100, easing = LinearEasing)), label = "t")
    val phases = remember { listOf(0f, .15f, .3f, .1f, .45f, .2f, .35f, .05f, .25f) }
    Canvas(modifier.width(64.dp).height(36.dp)) {
        val bar = 4.dp.toPx()
        val step = size.width / phases.size
        phases.forEachIndexed { i, phase ->
            val level = 0.25f + 0.75f * abs(sin(PI.toFloat() * (time + phase)))
            val h = size.height * level
            drawRoundRect(
                color.copy(alpha = 0.7f),
                topLeft = Offset(i * step + (step - bar) / 2, size.height - h),
                size = Size(bar, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bar / 2),
            )
        }
    }
}

/** The pill switch between the sections; the chosen one turns white. */
@Composable
internal fun SectionTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Palette.NightRaised)
            .border(1.dp, Palette.Outline, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val isOn = i == selected
            val background by animateColorAsState(if (isOn) Palette.Snow else Color.Transparent, tween(250), label = "tab-bg")
            val content by animateColorAsState(if (isOn) Palette.Night else Palette.Mist, tween(250), label = "tab-fg")
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(background)
                    .clickable { onSelect(i) }
                    .semantics {
                        role = Role.Tab
                        this.selected = isOn
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = content, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

/** The "Aujourd'hui" card: one action, a gradient night background and the dancing equalizer. */
@Composable
internal fun FocusCard(
    eyebrow: String,
    title: String,
    text: String?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Palette.VioletDeep, Palette.NightRaised, Palette.BlueDeep)))
            .border(1.dp, Color(0xFF3A3260), shape)
            .padding(20.dp),
    ) {
        Equalizer(Modifier.align(Alignment.TopEnd))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = Color(0xFFB9A2FF), modifier = Modifier.padding(end = 72.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 56.dp))
            text?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFC9C6DA)) }
            if (action != null) Box(Modifier.padding(top = 10.dp)) { action() }
        }
    }
}
