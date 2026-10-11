package com.xperiencemusic.shared.ui.stage

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
            if (state.setlistPosition != null) SetlistNavigation(state, onOpenSong)
        }
    }
}

@Composable
private fun StageToolbar(state: StageUiState, onEvent: (StageEvent) -> Unit, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Spacing.s)) {
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.stage_close)) }
        Column(Modifier.weight(1f)) {
            Text(state.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(state.keyLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        }
        IconButton(onClick = { onEvent(StageEvent.TextSmallerClicked) }) {
            Icon(StageIcons.Minus, contentDescription = stringResource(Res.string.stage_text_smaller))
        }
        Text("Aa", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onEvent(StageEvent.TextBiggerClicked) }) {
            Icon(StageIcons.Plus, contentDescription = stringResource(Res.string.stage_text_bigger))
        }
        IconButton(onClick = { onEvent(StageEvent.AutoScrollToggled) }) {
            if (state.isAutoScrolling) {
                Icon(PauseIcon, contentDescription = stringResource(Res.string.stage_scroll_stop))
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(Res.string.stage_scroll_start))
            }
        }
    }
    if (state.isAutoScrolling) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.s),
        ) {
            TextButton(onClick = { onEvent(StageEvent.SlowerClicked) }) { Text(stringResource(Res.string.stage_slower)) }
            Text("${state.scrollSpeed}", style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { onEvent(StageEvent.FasterClicked) }) { Text(stringResource(Res.string.stage_faster)) }
        }
    }
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
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(Spacing.s)) {
        IconButton(onClick = { state.previousSongId?.let(onOpenSong) }, enabled = state.previousSongId != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.stage_previous))
        }
        Text(
            state.setlistPosition.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = { state.nextSongId?.let(onOpenSong) }, enabled = state.nextSongId != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(Res.string.stage_next))
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
            ),
            onEvent = {},
            onClose = {},
            onOpenSong = {},
        )
    }
}
