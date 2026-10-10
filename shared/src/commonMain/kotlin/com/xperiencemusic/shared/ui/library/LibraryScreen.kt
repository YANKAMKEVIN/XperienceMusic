package com.xperiencemusic.shared.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.library.LibraryEvent
import com.xperiencemusic.shared.presentation.library.LibrarySongUi
import com.xperiencemusic.shared.presentation.library.LibraryUiState
import com.xperiencemusic.shared.presentation.library.LibraryViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.library_empty
import com.xperiencemusic.shared.resources.library_search
import com.xperiencemusic.shared.resources.library_title
import com.xperiencemusic.shared.resources.library_versions
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryScreen(onSongClick: (String) -> Unit, viewModel: LibraryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryScreenContent(state = state, onEvent = viewModel::onEvent, onSongClick = onSongClick)
}

@Composable
fun LibraryScreenContent(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onSongClick: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        item {
            Text(stringResource(Res.string.library_title), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onEvent(LibraryEvent.QueryChanged(it)) },
                placeholder = { Text(stringResource(Res.string.library_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!state.isLoading && state.songs.isEmpty()) {
            item {
                Text(
                    stringResource(Res.string.library_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.l),
                )
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
        Text(song.title, style = MaterialTheme.typography.titleMedium)
        song.artist?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            song.keyLabel + " · " + stringResource(Res.string.library_versions, song.recordingCount, song.maxRecordings),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = Spacing.xs),
        )
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
                    LibrarySongUi("1", "À toi la gloire", "E. Budry", "Ré majeur", 2, 3),
                    LibrarySongUi("2", "Amazing Grace", "John Newton", "Sol majeur", 1, 3),
                ),
            ),
            onEvent = {},
            onSongClick = {},
        )
    }
}

@Preview
@Composable
private fun LibraryScreenEmptyPreview() {
    AppTheme { LibraryScreenContent(LibraryUiState(query = "xyz", isLoading = false), onEvent = {}, onSongClick = {}) }
}
