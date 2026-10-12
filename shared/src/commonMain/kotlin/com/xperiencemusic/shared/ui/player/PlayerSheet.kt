package com.xperiencemusic.shared.ui.player

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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
import com.xperiencemusic.shared.resources.player_play
import com.xperiencemusic.shared.resources.player_position
import com.xperiencemusic.shared.resources.player_rewind
import com.xperiencemusic.shared.ui.brand.BrandWaves
import com.xperiencemusic.shared.ui.brand.XmMonogram
import com.xperiencemusic.shared.ui.components.PauseIcon
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

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
        Spacer(Modifier.height(Spacing.xxl))
        XmMonogram(size = 140.dp)
        BrandWaves(Modifier.fillMaxWidth().height(96.dp))
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
        Row(Modifier.fillMaxWidth()) {
            Text(state.positionLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(state.durationLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = Spacing.l),
        ) {
            val rewind = stringResource(Res.string.player_rewind)
            val forward = stringResource(Res.string.player_forward)
            IconButton(
                onClick = { onEvent(MiniPlayerEvent.RewindClicked) },
                modifier = Modifier.size(56.dp).semantics { contentDescription = rewind },
            ) {
                Text("−10", style = MaterialTheme.typography.titleMedium)
            }
            BigPlayButton(state, onClick = { onEvent(MiniPlayerEvent.PlayPauseClicked) })
            IconButton(
                onClick = { onEvent(MiniPlayerEvent.ForwardClicked) },
                modifier = Modifier.size(56.dp).semantics { contentDescription = forward },
            ) {
                Text("+10", style = MaterialTheme.typography.titleMedium)
            }
        }
        Text(
            stringResource(Res.string.player_background),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xl),
        )
    }
}

@Composable
private fun BigPlayButton(state: MiniPlayerUiState, onClick: () -> Unit) {
    val label = stringResource(if (state.isPlaying) Res.string.player_pause else Res.string.player_play, state.subtitle)
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(80.dp).clip(CircleShape).background(BrandColors.gradient),
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            state.isPlaying -> Icon(PauseIcon, contentDescription = label, tint = Color.White, modifier = Modifier.size(36.dp))
            else -> Icon(Icons.Filled.PlayArrow, contentDescription = label, tint = Color.White, modifier = Modifier.size(40.dp))
        }
    }
}
