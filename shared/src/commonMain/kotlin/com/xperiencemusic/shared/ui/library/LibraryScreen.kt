package com.xperiencemusic.shared.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.library.LibraryEvent
import com.xperiencemusic.shared.presentation.library.LibrarySectionHeader
import com.xperiencemusic.shared.presentation.library.LibrarySectionUi
import com.xperiencemusic.shared.presentation.library.LibrarySongUi
import com.xperiencemusic.shared.presentation.library.LibraryTab
import com.xperiencemusic.shared.presentation.library.LibraryUiState
import com.xperiencemusic.shared.presentation.library.LibraryViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_add_song
import com.xperiencemusic.shared.resources.library_empty
import com.xperiencemusic.shared.resources.library_empty_admin
import com.xperiencemusic.shared.resources.library_empty_member
import com.xperiencemusic.shared.resources.library_empty_title
import com.xperiencemusic.shared.resources.first_load_failed_text
import com.xperiencemusic.shared.resources.library_load_failed_title
import com.xperiencemusic.shared.resources.library_repertoire_empty
import com.xperiencemusic.shared.resources.library_repertoire_empty_title
import com.xperiencemusic.shared.resources.library_search
import com.xperiencemusic.shared.resources.library_section_alphabetical
import com.xperiencemusic.shared.resources.library_section_next
import com.xperiencemusic.shared.resources.library_section_recent
import com.xperiencemusic.shared.resources.library_section_results
import com.xperiencemusic.shared.resources.library_show_catalogue
import com.xperiencemusic.shared.resources.library_tab_catalogue
import com.xperiencemusic.shared.resources.library_tab_repertoire
import com.xperiencemusic.shared.resources.library_title
import com.xperiencemusic.shared.resources.offline_retry
import com.xperiencemusic.shared.ui.brand.TabHeader
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.ClearSearchButton
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.ui.components.LoadingIndicator
import com.xperiencemusic.shared.ui.components.OfflineBanner
import com.xperiencemusic.shared.ui.components.SongListRow
import com.xperiencemusic.shared.ui.components.choiceState
import com.xperiencemusic.shared.ui.format.rememberFormattingWords
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryScreen(
    onSongClick: (String) -> Unit,
    onAddSong: () -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryScreenContent(state = state, onEvent = viewModel::onEvent, onSongClick = onSongClick, onAddSong = onAddSong)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenContent(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onSongClick: (String) -> Unit,
    onAddSong: () -> Unit,
) {
    // In landscape the tab rail already names the screen: the height goes to the songs.
    BoxWithConstraints {
        val isShort = maxHeight < SHORT_SCREEN_HEIGHT
        Column(Modifier.fillMaxSize()) {
            // The search and the tabs stay in place: with a long catalogue they must not scroll away.
            LibraryHeader(state, isShort, onEvent, onAddSong)
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(LibraryEvent.RefreshPulled) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                LibraryList(state, showIndex = !isShort, onEvent, onSongClick, onAddSong)
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    state: LibraryUiState,
    isShort: Boolean,
    onEvent: (LibraryEvent) -> Unit,
    onAddSong: () -> Unit,
) {
    val addButton: @Composable () -> Unit = {
        if (state.canAddSong) {
            IconButton(onClick = onAddSong) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.action_add_song))
            }
        }
    }
    Column(
        Modifier.padding(start = Spacing.l, top = if (isShort) Spacing.s else Spacing.l, end = Spacing.l, bottom = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(if (isShort) Spacing.s else Spacing.m),
    ) {
        if (!isShort) TabHeader(stringResource(Res.string.library_title)) { addButton() }
        if (state.isOffline) OfflineBanner(onRetry = { onEvent(LibraryEvent.RefreshPulled) }, isRetrying = state.isRefreshing)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onEvent(LibraryEvent.QueryChanged(it)) },
                placeholder = { Text(stringResource(Res.string.library_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (state.query.isEmpty()) null else {
                    { ClearSearchButton { onEvent(LibraryEvent.QueryChanged("")) } }
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            if (isShort) addButton()
        }
        // A search covers every song, whatever the tab.
        if (!state.isSearching && state.catalogueCount > 0) LibraryTabs(state, onEvent)
    }
}

@Composable
private fun LibraryTabs(state: LibraryUiState, onEvent: (LibraryEvent) -> Unit) {
    val tabs = listOf(
        LibraryTab.REPERTOIRE to stringResource(Res.string.library_tab_repertoire, state.repertoireCount),
        LibraryTab.CATALOGUE to stringResource(Res.string.library_tab_catalogue, state.catalogueCount),
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, (tab, label) ->
            val selected = state.tab == tab
            SegmentedButton(
                selected = selected,
                onClick = { onEvent(LibraryEvent.TabSelected(tab)) },
                shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
                // The check mark would push the count out of a narrow screen.
                icon = {},
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.choiceState(selected),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryList(
    state: LibraryUiState,
    showIndex: Boolean,
    onEvent: (LibraryEvent) -> Unit,
    onSongClick: (String) -> Unit,
    onAddSong: () -> Unit,
) {
    val listState = rememberLazyListState()
    // A new tab or a new search starts from its top, not from where the previous list was.
    LaunchedEffect(state.tab, state.query) { listState.scrollToItem(0) }

    val letters = remember(state.sections) { letterPositions(state.sections) }
    val hasIndex = showIndex && letters.size >= MIN_INDEX_LETTERS

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            // Room to scroll the last results of a search above the keyboard.
            contentPadding = PaddingValues(bottom = Spacing.xxl + WindowInsets.ime.asPaddingValues().calculateBottomPadding()),
            modifier = Modifier.fillMaxSize(),
        ) {
            // The first load used to leave a blank list under the search field.
            if (state.isLoading) item { LoadingIndicator(Modifier.padding(top = Spacing.xxl)) }
            if (!state.isLoading && state.sections.isEmpty()) {
                item { LibraryEmpty(state, onEvent, onAddSong, Modifier.padding(horizontal = Spacing.l)) }
            }
            state.sections.forEachIndexed { sectionIndex, section ->
                stickyHeader(key = "header-$sectionIndex", contentType = "header") { SectionHeader(section.header) }
                // A song of the next service is also in the alphabetical list: the section keeps its key unique.
                items(section.songs, key = { "$sectionIndex-${it.id}" }, contentType = { "song" }) { song ->
                    SongListRow(
                        song,
                        onClick = { onSongClick(song.id) },
                        endPadding = if (hasIndex) INDEX_WIDTH else Spacing.l,
                    )
                }
            }
        }
        if (hasIndex) {
            val scope = rememberCoroutineScope()
            AlphabetIndex(
                // One character per section: a plain string keeps the parameter stable.
                letters = letters.joinToString("") { it.first },
                onLetter = { index -> scope.launch { listState.scrollToItem(letters[index].second) } },
                modifier = Modifier.align(Alignment.CenterEnd).padding(bottom = Spacing.xxl),
            )
        }
    }
}

/** Each letter with the position of its header in the list. */
private fun letterPositions(sections: List<LibrarySectionUi>): List<Pair<String, Int>> {
    var position = 0
    return buildList {
        sections.forEach { section ->
            (section.header as? LibrarySectionHeader.Letter)?.let { add(it.letter to position) }
            position += 1 + section.songs.size
        }
    }
}

@Composable
private fun LibraryEmpty(state: LibraryUiState, onEvent: (LibraryEvent) -> Unit, onAddSong: () -> Unit, modifier: Modifier = Modifier) {
    when {
        // Before the search: with no copy on the phone there is nothing to search in.
        state.hasLoadFailed -> EmptyState(
            icon = XmIcons.Book,
            title = stringResource(Res.string.library_load_failed_title),
            text = stringResource(Res.string.first_load_failed_text),
            modifier = modifier,
            actionLabel = stringResource(Res.string.offline_retry),
            onAction = { onEvent(LibraryEvent.RefreshPulled) },
        )
        state.isSearching -> Text(
            stringResource(Res.string.library_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxWidth().padding(top = Spacing.l),
        )
        state.catalogueCount == 0 -> EmptyState(
            icon = XmIcons.Book,
            title = stringResource(Res.string.library_empty_title),
            text = stringResource(if (state.canAddSong) Res.string.library_empty_admin else Res.string.library_empty_member),
            modifier = modifier,
            actionLabel = if (state.canAddSong) stringResource(Res.string.action_add_song) else null,
            onAction = onAddSong,
        )
        else -> EmptyState(
            icon = XmIcons.Book,
            title = stringResource(Res.string.library_repertoire_empty_title),
            text = stringResource(Res.string.library_repertoire_empty),
            modifier = modifier,
            actionLabel = stringResource(Res.string.library_show_catalogue),
            onAction = { onEvent(LibraryEvent.TabSelected(LibraryTab.CATALOGUE)) },
        )
    }
}

@Composable
private fun SectionHeader(header: LibrarySectionHeader) {
    val words = rememberFormattingWords()
    val text = when (header) {
        is LibrarySectionHeader.NextService -> stringResource(Res.string.library_section_next, words.shortDayAndDate(header.date))
        LibrarySectionHeader.RecentlySung -> stringResource(Res.string.library_section_recent)
        LibrarySectionHeader.Alphabetical -> stringResource(Res.string.library_section_alphabetical)
        is LibrarySectionHeader.Letter -> header.letter
        is LibrarySectionHeader.Results -> stringResource(Res.string.library_section_results, header.count)
    }
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        // Opaque: the rows scroll underneath while the header stays at the top.
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.l, vertical = Spacing.s)
            .semantics { heading() },
    )
}

/**
 * The letters down the right edge: a tap or a slide jumps to the songs of the letter under the
 * finger, which shows large in a bubble beside it, as in a phone's contact list. Screen readers
 * move by the section headings instead.
 */
@Composable
private fun AlphabetIndex(letters: String, onLetter: (Int) -> Unit, modifier: Modifier = Modifier) {
    val currentOnLetter by rememberUpdatedState(onLetter)
    val haptics = LocalHapticFeedback.current
    var isTouched by remember { mutableStateOf(false) }
    // Kept after the finger lifts, so the bubble fades out with its letter.
    var touchedLetter by remember { mutableStateOf("") }
    var touchedLetterY by remember { mutableFloatStateOf(0f) }
    Box(modifier.width(INDEX_WIDTH).heightIn(max = INDEX_LETTER_HEIGHT * letters.length).fillMaxHeight()) {
        Column(
            Modifier
                .fillMaxSize()
                .pointerInput(letters) {
                    var last = -1
                    fun pick(y: Float) {
                        val index = (y / size.height * letters.length).toInt().coerceIn(0, letters.lastIndex)
                        if (index == last) return
                        last = index
                        touchedLetter = letters[index].toString()
                        touchedLetterY = (index + 0.5f) * size.height / letters.length
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnLetter(index)
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        last = -1
                        isTouched = true
                        pick(down.position.y)
                        drag(down.id) { change ->
                            pick(change.position.y)
                            change.consume()
                        }
                        isTouched = false
                    }
                }
                .clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEach { letter ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        letter.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }
        }
        IndexBubble(touchedLetter, isVisible = isTouched, centerY = touchedLetterY)
    }
}

/** The letter under the finger, large, to the left of the index and level with that letter. */
@Composable
private fun IndexBubble(letter: String, isVisible: Boolean, centerY: Float) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        // Wider than the index it is laid out in: the offset moves it out to the left.
        modifier = Modifier
            .requiredSize(BUBBLE_SIZE)
            .offset {
                IntOffset(
                    x = -((BUBBLE_SIZE + INDEX_WIDTH) / 2 + Spacing.s).roundToPx(),
                    y = (centerY - BUBBLE_SIZE.toPx() / 2f).roundToInt(),
                )
            }
            .clearAndSetSemantics {},
    ) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(letter, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

private val BUBBLE_SIZE = 64.dp
private val SHORT_SCREEN_HEIGHT = 480.dp
private val INDEX_WIDTH = 28.dp
private val INDEX_LETTER_HEIGHT = 18.dp
/** Under this many letters the list is short enough to scroll. */
private const val MIN_INDEX_LETTERS = 5

@Preview
@Composable
private fun LibraryScreenRepertoirePreview() {
    AppTheme {
        LibraryScreenContent(
            LibraryUiState(
                isLoading = false,
                repertoireCount = 3,
                catalogueCount = 1947,
                sections = listOf(
                    LibrarySectionUi(
                        LibrarySectionHeader.NextService(LocalDate(2026, 11, 22)),
                        listOf(LibrarySongUi("1", "Tu es digne", null, hasAudio = true)),
                    ),
                    LibrarySectionUi(
                        LibrarySectionHeader.RecentlySung,
                        listOf(
                            LibrarySongUi("2", "À toi la gloire", "E. Budry", hasAudio = true, lastSungOn = LocalDate(2026, 11, 8)),
                            LibrarySongUi("3", "Amazing Grace", "John Newton", hasAudio = false, lastSungOn = LocalDate(2026, 11, 1)),
                        ),
                    ),
                ),
                canAddSong = true,
            ),
            onEvent = {},
            onSongClick = {},
            onAddSong = {},
        )
    }
}

@Preview
@Composable
private fun LibraryScreenCataloguePreview() {
    AppTheme {
        LibraryScreenContent(
            LibraryUiState(
                isLoading = false,
                tab = LibraryTab.CATALOGUE,
                repertoireCount = 3,
                catalogueCount = 6,
                sections = listOf("A", "B", "C", "G", "J", "T").mapIndexed { index, letter ->
                    LibrarySectionUi(
                        LibrarySectionHeader.Letter(letter),
                        listOf(LibrarySongUi("$index", "$letter comme chant", "Jeunesse en Mission", hasAudio = index == 0)),
                    )
                },
            ),
            onEvent = {},
            onSongClick = {},
            onAddSong = {},
        )
    }
}

@Preview
@Composable
private fun LibraryScreenEmptyPreview() {
    AppTheme { LibraryScreenContent(LibraryUiState(query = "xyz", isLoading = false), onEvent = {}, onSongClick = {}, onAddSong = {}) }
}
