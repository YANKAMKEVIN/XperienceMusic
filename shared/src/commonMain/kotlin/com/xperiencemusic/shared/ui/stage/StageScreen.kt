package com.xperiencemusic.shared.ui.stage

import com.xperiencemusic.shared.resources.stage_transposed
import com.xperiencemusic.shared.presentation.common.signedSemitones
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
import com.xperiencemusic.shared.resources.stage_text_bigger_glyph
import com.xperiencemusic.shared.resources.stage_text_smaller
import com.xperiencemusic.shared.resources.stage_text_smaller_glyph
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
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

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
    lyricsOnly: Boolean = false,
    viewModel: StageViewModel = koinViewModel(key = "stage-$songId-$transposeSemitones-$notation-$lyricsOnly") {
        parametersOf(songId, transposeSemitones, notation, lyricsOnly)
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
                        StageToolbar(state, onClose)
                        StageBody(state, onEvent, Modifier.weight(1f))
                    }
                    StageRail(state, onEvent, onOpenSong)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    StageToolbar(state, onClose)
                    StageBody(state, onEvent, Modifier.weight(1f))
                    StageControls(state, onEvent)
                    if (state.setlistPosition != null) SetlistNavigation(state, onOpenSong)
                }
            }
        }
    }
}

@Composable
private fun StageBody(state: StageUiState, onEvent: (StageEvent) -> Unit, modifier: Modifier = Modifier) {
    if (state.isLoading) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        StageLyrics(state, onToggleScroll = { onEvent(StageEvent.AutoScrollToggled) }, modifier)
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
                GlyphLabel(stringResource(Res.string.stage_text_smaller_glyph), textSmaller)
            }
            StageButton(onClick = { onEvent(StageEvent.TextBiggerClicked) }, modifier = Modifier.weight(1f), compact = true) {
                GlyphLabel(stringResource(Res.string.stage_text_bigger_glyph), textBigger)
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
private fun StageToolbar(state: StageUiState, onClose: () -> Unit) {
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
        // On stage the transposition chosen on the song page was invisible: the key now says how
        // far it is from the song's own key ("+2 · origine Ré majeur", short so the title keeps room).
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = Spacing.s, end = Spacing.m)) {
            val colors = MaterialTheme.colorScheme
            Text(
                state.keyLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (state.originalKeyLabel != null) colors.onSurface else colors.onSurfaceVariant,
            )
            state.originalKeyLabel?.let { original ->
                Text(
                    stringResource(Res.string.stage_transposed, signedSemitones(state.transposeSemitones), original),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }
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
            GlyphLabel(stringResource(Res.string.stage_text_smaller_glyph), textSmaller)
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
            GlyphLabel(stringResource(Res.string.stage_text_bigger_glyph), textBigger)
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

/**
 * The whole page is the pause button: with an instrument in the hands, anywhere on the lyrics is a
 * surer target than a button at the bottom. Scrolling by hand holds the auto-scroll, which
 * carries on from there once the finger is lifted.
 */
@Composable
private fun StageLyrics(state: StageUiState, onToggleScroll: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(state.isAutoScrolling, state.scrollSpeed) {
        if (!state.isAutoScrolling) return@LaunchedEffect
        val pixelsPerTick = with(density) { state.scrollSpeed.dp.toPx() } * TICK_MS / 1000f
        while (scroll.value < scroll.maxValue) {
            try {
                scroll.animateScrollBy(pixelsPerTick)
                delay(TICK_MS)
            } catch (interrupted: CancellationException) {
                // A finger took the page over, which used to end the auto-scroll for good while
                // the button still said it was running. Wait for the hand (and its fling) to finish.
                ensureActive()
                snapshotFlow { scroll.isScrollInProgress }.first { !it }
            }
        }
    }
    // How many taps: each one shows the play or pause mark for a moment.
    var taps by remember { mutableIntStateOf(0) }
    var showsTapMark by remember { mutableStateOf(false) }
    LaunchedEffect(taps) {
        if (taps == 0) return@LaunchedEffect
        showsTapMark = true
        delay(TAP_MARK_MS)
        showsTapMark = false
    }
    val tapLabel = stringResource(if (state.isAutoScrolling) Res.string.stage_scroll_stop else Res.string.stage_scroll_start)
    // A little room on top so the accent of a capital ("À") is not cut by the toolbar edge, and once
    // the text scrolls under it the edge softens: a line fades out instead of losing its accents first.
    val fadeHeight = with(density) { Spacing.l.toPx() }
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxSize()
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
                // No ripple: a flash across the whole page would hide the line being sung.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = tapLabel,
                    role = Role.Button,
                ) {
                    onToggleScroll()
                    taps++
                }
                .verticalScroll(scroll)
                .padding(horizontal = Spacing.l)
                .padding(top = Spacing.s),
        ) {
            ChordChartView(state.chart, scale = state.textScale, lyricsOnly = state.lyricsOnly)
            // Room so the last line can scroll up to eye level.
            Spacer(Modifier.height(LYRICS_END_ROOM))
        }
        AnimatedVisibility(
            visible = showsTapMark,
            enter = fadeIn(tween(TAP_MARK_FADE_MS)) + scaleIn(tween(TAP_MARK_FADE_MS), initialScale = 0.8f),
            exit = fadeOut(tween(TAP_MARK_FADE_MS * 2)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                Modifier.size(TAP_MARK_SIZE).background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // What the tap just did: the scroll runs (play) or holds (pause).
                Icon(
                    if (state.isAutoScrolling) Icons.Filled.PlayArrow else PauseIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Sizes.iconXxl),
                )
            }
        }
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
/** The play or pause mark in the middle of the lyrics after a tap, and how long it stays. */
private val TAP_MARK_SIZE = 88.dp
private const val TAP_MARK_MS = 650L
private const val TAP_MARK_FADE_MS = 140
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

@Composable
private fun GlyphLabel(glyph: String, description: String) {
    Text(glyph, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { contentDescription = description })
}
