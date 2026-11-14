package com.xperiencemusic.shared.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.common.PlaybackStatus
import com.xperiencemusic.shared.presentation.player.MiniPlayerEvent
import com.xperiencemusic.shared.presentation.player.MiniPlayerUiState
import com.xperiencemusic.shared.presentation.player.MiniPlayerViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.player_close
import com.xperiencemusic.shared.resources.player_error
import com.xperiencemusic.shared.resources.player_open
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.PlayPauseButton
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Radius
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.remember

/** The pill's corners; the progress line keeps clear of them by as much. */
private val CORNER_RADIUS = 22.dp
private val PROGRESS_HEIGHT = 2.dp

/**
 * A floating card above the tabs while something is loaded, so listening goes on across screens;
 * a tap opens the full player.
 */
@Composable
fun MiniPlayer(compact: Boolean = false, viewModel: MiniPlayerViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    // The card slides away showing what was playing, not the emptied state that hid it.
    val lastShown = remember { LastShown(state) }
    if (state.isVisible) lastShown.state = state
    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(),
    ) {
        MiniPlayerContent(
            state = lastShown.state,
            onEvent = viewModel::onEvent,
            onExpand = { isExpanded = true },
            compact = compact,
        )
    }
    if (state.isVisible) {
        if (isExpanded) PlayerSheet(state, viewModel::onEvent, onDismiss = { isExpanded = false })
    } else {
        isExpanded = false
    }
}

private class LastShown(var state: MiniPlayerUiState)

@Composable
fun MiniPlayerContent(
    state: MiniPlayerUiState,
    onEvent: (MiniPlayerEvent) -> Unit,
    onExpand: () -> Unit,
    /** Landscape: one slim line, without the artwork tile nor the expand arrow (the row opens it). */
    compact: Boolean = false,
) {
    val openLabel = stringResource(Res.string.player_open)
    val shape = RoundedCornerShape(CORNER_RADIUS)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = shape,
        border = BorderStroke(Sizes.hairline, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onExpand)
                    .padding(start = Spacing.s, end = Spacing.xs, top = if (compact) 0.dp else Spacing.s, bottom = if (compact) 0.dp else Spacing.s),
            ) {
                if (!compact) Box(
                    Modifier.size(Sizes.iconTile).background(BrandColors.gradient, RoundedCornerShape(Radius.chip)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(XmIcons.Notes, contentDescription = null, tint = BrandColors.onGradient, modifier = Modifier.size(Sizes.iconL))
                }
                Column(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
                    Text(state.subtitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (state.hasError) stringResource(Res.string.player_error) else state.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PlayPauseButton(
                    status = when {
                        state.isLoading -> PlaybackStatus.LOADING
                        state.isPlaying -> PlaybackStatus.PLAYING
                        else -> PlaybackStatus.PAUSED
                    },
                    itemLabel = state.title,
                    onClick = { onEvent(MiniPlayerEvent.PlayPauseClicked) },
                )
                if (!compact) IconButton(onClick = onExpand) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = openLabel)
                }
                IconButton(onClick = { onEvent(MiniPlayerEvent.CloseClicked) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.player_close))
                }
            }
            LinearProgressIndicator(
                progress = { state.progress },
                trackColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth().padding(horizontal = CORNER_RADIUS).height(PROGRESS_HEIGHT),
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
            onExpand = {},
        )
    }
}
