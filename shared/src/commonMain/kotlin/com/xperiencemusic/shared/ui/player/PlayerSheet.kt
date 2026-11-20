package com.xperiencemusic.shared.ui.player

import androidx.compose.foundation.background
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.xperiencemusic.shared.presentation.player.LoopUi
import com.xperiencemusic.shared.resources.player_loop_clear
import com.xperiencemusic.shared.resources.player_loop_mark_end
import com.xperiencemusic.shared.resources.player_loop_mark_start
import com.xperiencemusic.shared.resources.player_loop_none
import com.xperiencemusic.shared.resources.player_loop_running
import com.xperiencemusic.shared.resources.player_loop_start
import com.xperiencemusic.shared.resources.player_speed_normal
import com.xperiencemusic.shared.resources.player_speed_slow
import com.xperiencemusic.shared.resources.player_speed_slow_off
import com.xperiencemusic.shared.resources.player_speed_slow_on
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.xperiencemusic.shared.presentation.player.MiniPlayerEvent
import com.xperiencemusic.shared.presentation.player.MiniPlayerUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.player_background
import com.xperiencemusic.shared.resources.player_error
import com.xperiencemusic.shared.resources.player_forward
import com.xperiencemusic.shared.resources.player_pause
import com.xperiencemusic.shared.resources.player_next
import com.xperiencemusic.shared.resources.player_previous
import com.xperiencemusic.shared.resources.player_queue_position
import com.xperiencemusic.shared.resources.player_play
import com.xperiencemusic.shared.resources.player_position
import com.xperiencemusic.shared.resources.player_rewind
import com.xperiencemusic.shared.ui.brand.BrandWaves
import com.xperiencemusic.shared.ui.brand.XmMonogram
import com.xperiencemusic.shared.ui.components.PauseIcon
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.xperiencemusic.shared.presentation.common.PlaybackStatus

private val MONOGRAM_SIZE = 140.dp
private val WAVES_HEIGHT = 96.dp
/** The monogram's beat while playing: three hundredths larger, in and out in under two seconds. */
private const val BEAT_SCALE = 1.03f
private const val BEAT_MS = 850
private val PLAY_BUTTON_SIZE = 80.dp
private val PLAY_LOADER_SIZE = 28.dp
/** Material's play arrow has more air in its box than the pause glyph: drawn larger to match. */
private val PLAY_GLYPH_SIZE = 40.dp
/** The looped passage under the position bar, inset like the bar's own track. */
private val LOOP_BAR_HEIGHT = 4.dp
private val LOOP_BAR_INSET = 8.dp

/** The full player: the recording, the brand waves, and the controls to learn a part. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSheet(state: MiniPlayerUiState, onEvent: (MiniPlayerEvent) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        PlayerContent(state, onEvent)
    }
}

@Composable
fun PlayerContent(state: MiniPlayerUiState, onEvent: (MiniPlayerEvent) -> Unit) {
    // While the thumb is dragged, show the finger's position rather than the player's.
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val positionDescription = stringResource(Res.string.player_position)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.xl, vertical = Spacing.l),
    ) {
        Text(
            state.title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.2.em),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            state.subtitle,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
        )
        state.queuePosition?.let { position ->
            val description = stringResource(Res.string.player_queue_position, position)
            Text(
                position,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Spacing.xs).semantics { contentDescription = description },
            )
        }
        Spacer(Modifier.height(Spacing.xxl))
        val isSounding = state.isPlaying && !state.isLoading
        BeatingMonogram(isSounding)
        BrandWaves(Modifier.fillMaxWidth().height(WAVES_HEIGHT), isActive = isSounding)
        if (state.hasError) {
            Text(stringResource(Res.string.player_error), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Slider(
            value = dragFraction ?: state.progress,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onEvent(MiniPlayerEvent.Seeked(it)) }
                dragFraction = null
            },
            enabled = state.durationLabel.isNotEmpty(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.padding(top = Spacing.l).semantics { contentDescription = positionDescription },
        )
        state.loop?.let { LoopBar(it) }
        Row(Modifier.fillMaxWidth()) {
            Text(state.positionLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(state.durationLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // With a setlist, previous and next frame the controls and the row spreads to make room.
        val hasQueue = state.queuePosition != null
        Row(
            horizontalArrangement = if (hasQueue) Arrangement.SpaceBetween else Arrangement.spacedBy(Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = Spacing.l).then(if (hasQueue) Modifier.fillMaxWidth() else Modifier),
        ) {
            val rewind = stringResource(Res.string.player_rewind)
            val forward = stringResource(Res.string.player_forward)
            val sideButton = if (hasQueue) Sizes.touchTarget else Sizes.rowMin
            if (hasQueue) {
                // Never disabled: on the first song it still restarts the track.
                IconButton(onClick = { onEvent(MiniPlayerEvent.PreviousClicked) }, modifier = Modifier.size(sideButton)) {
                    Icon(PlayerIcons.SkipPrevious, contentDescription = stringResource(Res.string.player_previous))
                }
            }
            IconButton(
                onClick = { onEvent(MiniPlayerEvent.RewindClicked) },
                modifier = Modifier.size(sideButton).semantics { contentDescription = rewind },
            ) {
                Text("−10", style = MaterialTheme.typography.titleMedium)
            }
            BigPlayButton(state, onClick = { onEvent(MiniPlayerEvent.PlayPauseClicked) })
            IconButton(
                onClick = { onEvent(MiniPlayerEvent.ForwardClicked) },
                modifier = Modifier.size(sideButton).semantics { contentDescription = forward },
            ) {
                Text("+10", style = MaterialTheme.typography.titleMedium)
            }
            if (hasQueue) {
                IconButton(
                    onClick = { onEvent(MiniPlayerEvent.NextClicked) },
                    enabled = state.hasNext,
                    modifier = Modifier.size(sideButton),
                ) {
                    Icon(PlayerIcons.SkipNext, contentDescription = stringResource(Res.string.player_next))
                }
            }
        }
        PracticeControls(state, onEvent)
        Text(
            stringResource(Res.string.player_background),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xl),
        )
    }
}

/** The monogram breathes, barely, while the music plays, and comes to rest with it. */
@Composable
private fun BeatingMonogram(isPlaying: Boolean) {
    val beat = remember { Animatable(1f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            beat.animateTo(1f, tween(BEAT_MS))
            return@LaunchedEffect
        }
        while (true) {
            beat.animateTo(BEAT_SCALE, tween(BEAT_MS, easing = FastOutSlowInEasing))
            beat.animateTo(1f, tween(BEAT_MS, easing = FastOutSlowInEasing))
        }
    }
    XmMonogram(
        Modifier.graphicsLayer {
            scaleX = beat.value
            scaleY = beat.value
        },
        size = MONOGRAM_SIZE,
    )
}

/** Where the looped passage sits in the track, under the position bar: A alone is a dot, A to B a segment. */
@Composable
private fun LoopBar(loop: LoopUi) {
    val brush = BrandColors.horizontalGradient
    Canvas(Modifier.fillMaxWidth().padding(horizontal = LOOP_BAR_INSET).height(LOOP_BAR_HEIGHT)) {
        val start = size.width * loop.startFraction
        val end = loop.endFraction?.let { size.width * it }
        val radius = size.height / 2
        if (end == null) {
            drawCircle(brush, radius, Offset(start, radius))
        } else {
            drawRoundRect(brush, Offset(start, 0f), Size((end - start).coerceAtLeast(size.height), size.height), CornerRadius(radius))
        }
    }
}

/** To learn a part: the slow tempo, and a passage played over and over. */
@Composable
private fun PracticeControls(state: MiniPlayerUiState, onEvent: (MiniPlayerEvent) -> Unit) {
    val loop = state.loop
    val speedDescription = stringResource(if (state.isSlowed) Res.string.player_speed_slow_on else Res.string.player_speed_slow_off)
    val loopDescription = when {
        loop == null -> stringResource(Res.string.player_loop_mark_start)
        loop.endLabel == null -> stringResource(Res.string.player_loop_mark_end)
        else -> stringResource(Res.string.player_loop_clear, loop.startLabel, loop.endLabel)
    }
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.l),
    ) {
        FilterChip(
            selected = state.isSlowed,
            onClick = { onEvent(MiniPlayerEvent.SpeedClicked) },
            label = { Text(stringResource(if (state.isSlowed) Res.string.player_speed_slow else Res.string.player_speed_normal)) },
            colors = chipColors,
            modifier = Modifier.semantics { contentDescription = speedDescription },
        )
        FilterChip(
            selected = loop?.isRunning == true,
            onClick = { onEvent(MiniPlayerEvent.LoopClicked) },
            enabled = state.durationLabel.isNotEmpty(),
            label = {
                Text(
                    when {
                        loop == null -> stringResource(Res.string.player_loop_none)
                        loop.endLabel == null -> stringResource(Res.string.player_loop_start, loop.startLabel)
                        else -> stringResource(Res.string.player_loop_running, loop.startLabel, loop.endLabel)
                    },
                )
            },
            colors = chipColors,
            modifier = Modifier.semantics { contentDescription = loopDescription },
        )
    }
}

@Composable
private fun BigPlayButton(state: MiniPlayerUiState, onClick: () -> Unit) {
    val label = stringResource(if (state.isPlaying) Res.string.player_pause else Res.string.player_play, state.subtitle)
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(PLAY_BUTTON_SIZE).clip(CircleShape).background(BrandColors.gradient),
    ) {
        val shown = when {
            state.isLoading -> PlaybackStatus.LOADING
            state.isPlaying -> PlaybackStatus.PLAYING
            else -> PlaybackStatus.PAUSED
        }
        Crossfade(targetState = shown, animationSpec = tween(180), label = "big-play") { visual ->
            when (visual) {
                PlaybackStatus.LOADING -> CircularProgressIndicator(color = BrandColors.onGradient, strokeWidth = Sizes.stroke, modifier = Modifier.size(PLAY_LOADER_SIZE))
                PlaybackStatus.PLAYING -> Icon(PauseIcon, contentDescription = label, tint = BrandColors.onGradient, modifier = Modifier.size(Sizes.iconXxl))
                else -> Icon(Icons.Filled.PlayArrow, contentDescription = label, tint = BrandColors.onGradient, modifier = Modifier.size(PLAY_GLYPH_SIZE))
            }
        }
    }
}
