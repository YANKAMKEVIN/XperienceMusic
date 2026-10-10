package com.xperiencemusic.shared.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.common.PlaybackStatus
import com.xperiencemusic.shared.presentation.player.MiniPlayerEvent
import com.xperiencemusic.shared.presentation.player.MiniPlayerUiState
import com.xperiencemusic.shared.presentation.player.MiniPlayerViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.player_close
import com.xperiencemusic.shared.resources.player_error
import com.xperiencemusic.shared.resources.player_position
import com.xperiencemusic.shared.ui.components.PlayPauseButton
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Shown above the tabs while something is loaded, so listening goes on across screens.
 * [isAtScreenBottom]: nothing below it, so it must keep clear of the system navigation bar.
 */
@Composable
fun MiniPlayer(isAtScreenBottom: Boolean, viewModel: MiniPlayerViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.isVisible) {
        MiniPlayerContent(state = state, onEvent = viewModel::onEvent, isAtScreenBottom = isAtScreenBottom)
    }
}

@Composable
fun MiniPlayerContent(state: MiniPlayerUiState, onEvent: (MiniPlayerEvent) -> Unit, isAtScreenBottom: Boolean = false) {
    // While the thumb is dragged, show the finger's position rather than the player's.
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val positionDescription = stringResource(Res.string.player_position)

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .then(if (isAtScreenBottom) Modifier.navigationBarsPadding() else Modifier)
                .padding(start = Spacing.l, end = Spacing.s, top = Spacing.s),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayPauseButton(
                    status = when {
                        state.isLoading -> PlaybackStatus.LOADING
                        state.isPlaying -> PlaybackStatus.PLAYING
                        else -> PlaybackStatus.PAUSED
                    },
                    itemLabel = state.title,
                    onClick = { onEvent(MiniPlayerEvent.PlayPauseClicked) },
                )
                Column(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
                    Text(state.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (state.hasError) stringResource(Res.string.player_error) else state.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    if (state.durationLabel.isEmpty()) state.positionLabel else "${state.positionLabel} / ${state.durationLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = { onEvent(MiniPlayerEvent.CloseClicked) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.player_close))
                }
            }
            Slider(
                value = dragFraction ?: state.progress,
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    dragFraction?.let { onEvent(MiniPlayerEvent.Seeked(it)) }
                    dragFraction = null
                },
                enabled = state.durationLabel.isNotEmpty(),
                modifier = Modifier.semantics { contentDescription = positionDescription },
            )
        }
    }
}

@Preview
@Composable
private fun MiniPlayerPreview() {
    AppTheme {
        MiniPlayerContent(
            MiniPlayerUiState(
                isVisible = true,
                title = "Répétition du mardi",
                subtitle = "À toi la gloire",
                isPlaying = true,
                progress = 0.4f,
                positionLabel = "1:25",
                durationLabel = "3:32",
            ),
            onEvent = {},
        )
    }
}
