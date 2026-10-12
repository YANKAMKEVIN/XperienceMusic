package com.xperiencemusic.shared.ui.stage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.xperiencemusic.shared.ui.theme.Radius
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.music.chart.ChartLine
import com.xperiencemusic.music.chart.ChartSegment
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.presentation.stage.StageEvent
import com.xperiencemusic.shared.presentation.stage.StageUiState
import com.xperiencemusic.shared.presentation.stage.StageViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.stage_close
import com.xperiencemusic.shared.resources.stage_faster
import com.xperiencemusic.shared.resources.stage_next
import com.xperiencemusic.shared.resources.stage_previous
import com.xperiencemusic.shared.resources.stage_scroll_start
import com.xperiencemusic.shared.resources.stage_scroll_stop
import com.xperiencemusic.shared.resources.stage_slower
import com.xperiencemusic.shared.resources.stage_text_bigger
import com.xperiencemusic.shared.resources.stage_text_smaller
import com.xperiencemusic.shared.ui.components.PauseIcon
import com.xperiencemusic.shared.ui.components.StageIcons
import com.xperiencemusic.shared.ui.platform.HideSystemBars
import com.xperiencemusic.shared.ui.platform.KeepScreenOn
import com.xperiencemusic.shared.ui.song.ChordChartView
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun StageScreen(
    songId: String,
    transposeSemitones: Int,
    notation: Notation,
    onClose: () -> Unit,
    onOpenSong: (String) -> Unit,
    viewModel: StageViewModel = koinViewModel(key = "stage-$songId-$transposeSemitones-$notation") {
        parametersOf(songId, transposeSemitones, notation)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    KeepScreenOn()
    HideSystemBars()
    StageScreenContent(state = state, onEvent = viewModel::onEvent, onClose = onClose, onOpenSong = onOpenSong)
}

@Composable
fun StageScreenContent(
    state: StageUiState,
    onEvent: (StageEvent) -> Unit,
    onClose: () -> Unit,
    onOpenSong: (String) -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding()) {
            StageToolbar(state, onEvent, onClose)
            if (state.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                StageLyrics(state, Modifier.weight(1f))
            }
            StageControls(state, onEvent)
            if (state.setlistPosition != null) SetlistNavigation(state, onOpenSong)
        }
    }
}

@Composable
private fun StageToolbar(state: StageUiState, onEvent: (StageEvent) -> Unit, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Spacing.s)) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.stage_close))
        }
        Column(Modifier.weight(1f)) {
            Text(state.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            state.setlistPosition?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            state.keyLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = Spacing.m),
        )
    }
}

/** Text size and auto-scroll, big enough to hit on a music stand. */
@Composable
private fun StageControls(state: StageUiState, onEvent: (StageEvent) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
    ) {
        StageButton(onClick = { onEvent(StageEvent.TextSmallerClicked) }, modifier = Modifier.width(64.dp)) {
            Text("A−", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics {
                contentDescription = "Réduire le texte"
            })
        }
        StageButton(onClick = { onEvent(StageEvent.AutoScrollToggled) }, modifier = Modifier.weight(1f)) {
            Icon(
                if (state.isAutoScrolling) PauseIcon else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.padding(start = Spacing.s)) {
                Text(
                    stringResource(if (state.isAutoScrolling) Res.string.stage_scroll_stop else Res.string.stage_scroll_start),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                if (state.isAutoScrolling) {
                    Text("${state.scrollSpeed}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        StageButton(onClick = { onEvent(StageEvent.TextBiggerClicked) }, modifier = Modifier.width(64.dp)) {
            Text("A+", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics {
                contentDescription = "Agrandir le texte"
            })
        }
    }
    if (state.isAutoScrolling) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = { onEvent(StageEvent.SlowerClicked) }) { Text(stringResource(Res.string.stage_slower)) }
            TextButton(onClick = { onEvent(StageEvent.FasterClicked) }) { Text(stringResource(Res.string.stage_faster)) }
        }
    }
}

@Composable
private fun StageButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.card),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.m),
        modifier = modifier.height(56.dp),
        content = content,
    )
}

@Composable
private fun StageLyrics(state: StageUiState, modifier: Modifier) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(state.isAutoScrolling, state.scrollSpeed) {
        if (!state.isAutoScrolling) return@LaunchedEffect
        val pixelsPerTick = with(density) { state.scrollSpeed.dp.toPx() } * TICK_MS / 1000f
        while (scroll.value < scroll.maxValue) {
            scroll.animateScrollBy(pixelsPerTick)
            delay(TICK_MS)
        }
    }
    Column(modifier.fillMaxWidth().verticalScroll(scroll).padding(horizontal = Spacing.l)) {
        ChordChartView(state.chart, scale = state.textScale)
        // Room so the last line can scroll up to eye level.
        Spacer(Modifier.height(240.dp))
    }
}

@Composable
private fun SetlistNavigation(state: StageUiState, onOpenSong: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.l, end = Spacing.l, bottom = Spacing.s),
    ) {
        StageButton(
            onClick = { state.previousSongId?.let(onOpenSong) },
            modifier = Modifier.weight(1f).alpha(if (state.previousSongId != null) 1f else 0.4f),
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            Text(stringResource(Res.string.stage_previous), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        StageButton(
            onClick = { state.nextSongId?.let(onOpenSong) },
            modifier = Modifier.weight(1f).alpha(if (state.nextSongId != null) 1f else 0.4f),
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(stringResource(Res.string.stage_next), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                state.nextSongTitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

private const val TICK_MS = 50L

@Preview
@Composable
private fun StageScreenPreview() {
    AppTheme {
        StageScreenContent(
            StageUiState(
                isLoading = false,
                title = "À toi la gloire",
                keyLabel = "Ré majeur",
                chart = listOf(
                    ChartLine.Section("Refrain"),
                    ChartLine.Lyrics(listOf(ChartSegment("Ré", "À toi la "), ChartSegment("La", "gloire"))),
                ),
                setlistPosition = "1 / 3",
                nextSongId = "2",
                nextSongTitle = "Holy, Holy, Holy",
            ),
            onEvent = {},
            onClose = {},
            onOpenSong = {},
        )
    }
}
