package com.xperiencemusic.shared.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.ui.brand.KeyPill
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Radius
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.library.LibraryEvent
import com.xperiencemusic.shared.presentation.library.LibrarySongUi
import com.xperiencemusic.shared.presentation.library.LibraryUiState
import com.xperiencemusic.shared.presentation.library.LibraryViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_add_song
import com.xperiencemusic.shared.resources.library_all_keys
import com.xperiencemusic.shared.resources.library_empty
import com.xperiencemusic.shared.resources.library_empty_admin
import com.xperiencemusic.shared.resources.library_empty_member
import com.xperiencemusic.shared.resources.library_empty_title
import com.xperiencemusic.shared.resources.library_search
import com.xperiencemusic.shared.resources.library_title
import com.xperiencemusic.shared.resources.library_versions
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.ChoiceChips
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.ui.components.OfflineBanner
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(LibraryEvent.RefreshPulled) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LibraryList(state, onEvent, onSongClick, onAddSong)
    }
}

@Composable
private fun LibraryList(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onSongClick: (String) -> Unit,
    onAddSong: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.l, top = Spacing.l, end = Spacing.l, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.library_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (state.canAddSong) AddSongButton(onAddSong)
            }
        }
        if (state.isOffline) item { OfflineBanner() }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onEvent(LibraryEvent.QueryChanged(it)) },
                placeholder = { Text(stringResource(Res.string.library_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(Radius.card),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.keys.isNotEmpty()) {
            item {
                val allKeys = stringResource(Res.string.library_all_keys)
                ChoiceChips(
                    options = listOf(null) + state.keys,
                    selected = state.selectedKey,
                    label = { it ?: allKeys },
                    onSelect = { onEvent(LibraryEvent.KeySelected(it)) },
                )
            }
        }
        if (!state.isLoading && state.songs.isEmpty()) {
            item {
                if (state.query.isBlank() && state.selectedKey == null) {
                    EmptyState(
                        icon = XmIcons.Book,
                        title = stringResource(Res.string.library_empty_title),
                        text = stringResource(if (state.canAddSong) Res.string.library_empty_admin else Res.string.library_empty_member),
                        actionLabel = if (state.canAddSong) stringResource(Res.string.action_add_song) else null,
                        onAction = onAddSong,
                    )
                } else {
                    Text(
                        stringResource(Res.string.library_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.l),
                    )
                }
            }
        }
        items(state.songs, key = { it.id }) { song ->
            LibrarySongRow(song, onClick = { onSongClick(song.id) })
        }
    }
}

@Composable
private fun LibrarySongRow(song: LibrarySongUi, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(XmIcons.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.xs)) {
                    KeyPill(song.keyLabel)
                    Text(
                        listOfNotNull(
                            stringResource(Res.string.library_versions, song.recordingCount, song.maxRecordings),
                            song.artist,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = Spacing.s),
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The gradient "+" of the mockups; admins only. */
@Composable
private fun AddSongButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BrandColors.gradient)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.action_add_song), tint = Color.White)
    }
}

@Preview
@Composable
private fun LibraryScreenPreview() {
    AppTheme {
        LibraryScreenContent(
            LibraryUiState(
                isLoading = false,
                songs = listOf(
                    LibrarySongUi("1", "À toi la gloire", "E. Budry", "Ré", 2, 3),
                    LibrarySongUi("2", "Amazing Grace", "John Newton", "Sol", 1, 3),
                ),
                keys = listOf("Ré", "Sol", "Lam"),
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
private fun LibraryScreenEmptyPreview() {
    AppTheme { LibraryScreenContent(LibraryUiState(query = "xyz", isLoading = false), onEvent = {}, onSongClick = {}, onAddSong = {}) }
}
