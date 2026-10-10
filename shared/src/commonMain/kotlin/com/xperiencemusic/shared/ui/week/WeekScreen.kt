package com.xperiencemusic.shared.ui.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.week.WeekSetlistUi
import com.xperiencemusic.shared.presentation.week.WeekSongUi
import com.xperiencemusic.shared.presentation.week.WeekUiState
import com.xperiencemusic.shared.presentation.week.WeekViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.week_empty
import com.xperiencemusic.shared.resources.week_published_by
import com.xperiencemusic.shared.resources.week_rehearsal
import com.xperiencemusic.shared.resources.week_service
import com.xperiencemusic.shared.resources.week_song_has_reference
import com.xperiencemusic.shared.resources.week_song_new
import com.xperiencemusic.shared.resources.week_title
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WeekScreen(onSongClick: (String) -> Unit, viewModel: WeekViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WeekScreenContent(state = state, onSongClick = onSongClick)
}

@Composable
fun WeekScreenContent(state: WeekUiState, onSongClick: (String) -> Unit) {
    val setlist = state.setlist
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        setlist == null -> Box(Modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.week_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> WeekSetlist(setlist, onSongClick)
    }
}

@Composable
private fun WeekSetlist(setlist: WeekSetlistUi, onSongClick: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        item {
            Text(stringResource(Res.string.week_title), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            AppCard {
                Text(
                    stringResource(Res.string.week_rehearsal, setlist.rehearsalDate),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(Res.string.week_service, setlist.serviceDate),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                setlist.message?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.s))
                }
                Text(
                    stringResource(Res.string.week_published_by, setlist.publishedBy),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.s),
                )
            }
        }
        items(setlist.songs, key = { it.id }) { song ->
            WeekSongRow(song, onClick = { onSongClick(song.id) })
        }
    }
}

@Composable
private fun WeekSongRow(song: WeekSongUi, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${song.position}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(Spacing.l))
            Column {
                Text(song.title, style = MaterialTheme.typography.titleMedium)
                Text(song.keyLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Text(
                    stringResource(if (song.hasReference) Res.string.week_song_has_reference else Res.string.week_song_new),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun WeekScreenPreview() {
    AppTheme {
        WeekScreenContent(
            WeekUiState(
                isLoading = false,
                setlist = WeekSetlistUi(
                    rehearsalDate = "mardi 14 oct.",
                    serviceDate = "dimanche 19 oct.",
                    message = "On reprend l'intro acoustique.",
                    publishedBy = "Responsable musical",
                    songs = listOf(
                        WeekSongUi("1", 1, "À toi la gloire", "Ré majeur", hasReference = true),
                        WeekSongUi("2", 2, "Holy, Holy, Holy", "Ré majeur", hasReference = false),
                    ),
                ),
            ),
            onSongClick = {},
        )
    }
}

@Preview
@Composable
private fun WeekScreenEmptyPreview() {
    AppTheme { WeekScreenContent(WeekUiState(isLoading = false), onSongClick = {}) }
}
