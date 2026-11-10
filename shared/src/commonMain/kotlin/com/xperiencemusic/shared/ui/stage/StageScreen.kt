package com.xperiencemusic.shared.ui.stage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.xperiencemusic.shared.resources.stage_text_bigger
import com.xperiencemusic.shared.resources.stage_text_smaller
import com.xperiencemusic.shared.ui.theme.Radius
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import com.xperiencemusic.shared.resources.stage_speed
import com.xperiencemusic.shared.resources.stage_previous
import com.xperiencemusic.shared.resources.stage_scroll_start
import com.xperiencemusic.shared.resources.stage_scroll_stop
import com.xperiencemusic.shared.resources.stage_slower
import com.xperiencemusic.shared.ui.components.PauseIcon
import com.xperiencemusic.shared.ui.components.StageIcons
import com.xperiencemusic.shared.ui.platform.HideSystemBars
import com.xperiencemusic.shared.ui.platform.KeepScreenOn
import com.xperiencemusic.shared.ui.song.ChordChartView
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

/** The square-ish buttons of the controls bar (text size, speed). */
private val STAGE_BUTTON_WIDTH = 64.dp
/** Room under the last line so that it can scroll up to eye level. */
private val LYRICS_END_ROOM = 240.dp

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
        BoxWithConstraints(Modifier.safeDrawingPadding()) {
            // On a music stand the phone lies sideways: the controls move to a narrow rail on the
            // right so the lyrics keep the whole height.
            if (maxWidth > maxHeight) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f)) {
                        StageToolbar(state, onEvent, onClose)
                        StageBody(state, Modifier.weight(1f))
                    }
                    StageRail(state, onEvent, onOpenSong)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    StageToolbar(state, onEvent, onClose)
                    StageBody(state, Modifier.weight(1f))
                    StageControls(state, onEvent)
                    if (state.setlistPosition != null) SetlistNavigation(state, onOpenSong)
                }
            }
        }
    }
}

@Composable
private fun StageBody(state: StageUiState, modifier: Modifier) {
    if (state.isLoading) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        StageLyrics(state, modifier)
    }
}

/** Landscape: every control in a column of compact buttons beside the lyrics. */
@Composable
private fun StageRail(state: StageUiState, onEvent: (StageEvent) -> Unit, onOpenSong: (String) -> Unit) {
    val textSmaller = stringResource(Res.string.stage_text_smaller)
    val textBigger = stringResource(Res.string.stage_text_bigger)
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight().width(RAIL_WIDTH).verticalScroll(rememberScrollState()).padding(Spacing.s),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            StageButton(onClick = { onEvent(StageEvent.TextSmallerClicked) }, modifier = Modifier.weight(1f), compact = true) {
                Text("A−", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { contentDescription = textSmaller })
            }
            StageButton(onClick = { onEvent(StageEvent.TextBiggerClicked) }, modifier = Modifier.weight(1f), compact = true) {
                Text("A+", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { contentDescription = textBigger })
            }
        }
        StageButton(onClick = { onEvent(StageEvent.AutoScrollToggled) }, modifier = Modifier.fillMaxWidth(), compact = true) {
            Icon(
                if (state.isAutoScrolling) PauseIcon else Icons.Filled.PlayArrow,
                contentDescription = stringResource(if (state.isAutoScrolling) Res.string.stage_scroll_stop else Res.string.stage_scroll_start),
                tint = MaterialTheme.colorScheme.primary,
            )
            if (state.isAutoScrolling) {
                Text(
                    stringResource(Res.string.stage_speed, state.speedLevel, StageUiState.MAX_LEVEL),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = Spacing.xs),
                )
            }
        }
        if (state.isAutoScrolling) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                StageButton(onClick = { onEvent(StageEvent.SlowerClicked) }, modifier = Modifier.weight(1f), compact = true) {
                    Icon(StageIcons.Minus, contentDescription = stringResource(Res.string.stage_slower))
                }
                StageButton(onClick = { onEvent(StageEvent.FasterClicked) }, modifier = Modifier.weight(1f), compact = true) {
                    Icon(StageIcons.Plus, contentDescription = stringResource(Res.string.stage_faster))
                }
            }
        }
        if (state.setlistPosition != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                StageButton(
                    onClick = { state.previousSongId?.let(onOpenSong) },
                    modifier = Modifier.weight(1f).alpha(if (state.previousSongId != null) 1f else 0.4f),
                    compact = true,
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.stage_previous))
                }
                StageButton(
                    onClick = { state.nextSongId?.let(onOpenSong) },
                    modifier = Modifier.weight(1f).alpha(if (state.nextSongId != null) 1f else 0.4f),
                    compact = true,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = listOfNotNull(stringResource(Res.string.stage_next), state.nextSongTitle).joinToString(" : "),
                    )
                }
            }
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
    val textSmaller = stringResource(Res.string.stage_text_smaller)
    val textBigger = stringResource(Res.string.stage_text_bigger)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
    ) {
        StageButton(onClick = { onEvent(StageEvent.TextSmallerClicked) }, modifier = Modifier.width(STAGE_BUTTON_WIDTH)) {
            Text("A−", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { contentDescription = textSmaller })
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
                    Text(
                        stringResource(Res.string.stage_speed, state.speedLevel, StageUiState.MAX_LEVEL),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        StageButton(onClick = { onEvent(StageEvent.TextBiggerClicked) }, modifier = Modifier.width(STAGE_BUTTON_WIDTH)) {
            Text("A+", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { contentDescription = textBigger })
        }
    }
    // The speed buttons unfold under the controls rather than shoving the lyrics down at once.
    AnimatedVisibility(
        visible = state.isAutoScrolling,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            StageButton(onClick = { onEvent(StageEvent.SlowerClicked) }, modifier = Modifier.width(STAGE_BUTTON_WIDTH)) {
                Icon(StageIcons.Minus, contentDescription = stringResource(Res.string.stage_slower))
            }
            StageButton(onClick = { onEvent(StageEvent.FasterClicked) }, modifier = Modifier.padding(start = Spacing.s).width(STAGE_BUTTON_WIDTH)) {
                Icon(StageIcons.Plus, contentDescription = stringResource(Res.string.stage_faster))
            }
        }
    }
}

@Composable
private fun StageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.card),
        border = BorderStroke(Sizes.hairline, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = if (compact) Spacing.xs else Spacing.m),
        modifier = modifier.height(if (compact) Sizes.touchTarget else Sizes.rowMin),
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
    // A little room on top so the accent of a capital ("À") is not cut by the toolbar edge, and once
    // the text scrolls under it the edge softens: a line fades out instead of losing its accents first.
    val fadeHeight = with(density) { Spacing.l.toPx() }
    Column(
        modifier
            .fillMaxWidth()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val fade = minOf(scroll.value.toFloat(), fadeHeight)
                if (fade > 0f) {
                    drawRect(
                        brush = Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, endY = fade),
                        size = size.copy(height = fade),
                        blendMode = BlendMode.DstIn,
                    )
                }
            }
            .verticalScroll(scroll)
            .padding(horizontal = Spacing.l)
            .padding(top = Spacing.s),
    ) {
        ChordChartView(state.chart, scale = state.textScale)
        // Room so the last line can scroll up to eye level.
        Spacer(Modifier.height(LYRICS_END_ROOM))
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
private val RAIL_WIDTH = 120.dp

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

@Preview(widthDp = 780, heightDp = 360)
@Composable
private fun StageScreenLandscapePreview() {
    AppTheme {
        StageScreenContent(
            StageUiState(
                isLoading = false,
                title = "À toi la gloire",
                keyLabel = "Ré majeur",
                chart = listOf(
                    ChartLine.Section("Refrain"),
                    ChartLine.Lyrics(listOf(ChartSegment("Ré", "À toi la "), ChartSegment("La", "gloire, Ô Ressus", continuesWord = true), ChartSegment("Si m", "cité !"))),
                ),
                isAutoScrolling = true,
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
