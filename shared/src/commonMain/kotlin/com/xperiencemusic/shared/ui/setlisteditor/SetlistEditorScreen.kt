package com.xperiencemusic.shared.ui.setlisteditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.setlisteditor.SetlistEditorError
import com.xperiencemusic.shared.presentation.setlisteditor.SetlistEditorEvent
import com.xperiencemusic.shared.presentation.setlisteditor.SetlistEditorUiState
import com.xperiencemusic.shared.presentation.setlisteditor.SetlistEditorViewModel
import com.xperiencemusic.shared.presentation.setlisteditor.SetlistSongUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.error_forbidden
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.library_search
import com.xperiencemusic.shared.resources.setlist_add
import com.xperiencemusic.shared.resources.setlist_add_title
import com.xperiencemusic.shared.resources.setlist_change_date
import com.xperiencemusic.shared.resources.setlist_date_conflict
import com.xperiencemusic.shared.resources.setlist_dates_title
import com.xperiencemusic.shared.resources.setlist_editor_title
import com.xperiencemusic.shared.resources.setlist_error_invalid
import com.xperiencemusic.shared.resources.setlist_message_label
import com.xperiencemusic.shared.resources.setlist_move_down
import com.xperiencemusic.shared.resources.setlist_move_up
import com.xperiencemusic.shared.resources.setlist_publish
import com.xperiencemusic.shared.resources.setlist_rehearsal_label
import com.xperiencemusic.shared.resources.setlist_remove
import com.xperiencemusic.shared.resources.setlist_selected_empty
import com.xperiencemusic.shared.resources.setlist_selected_title
import com.xperiencemusic.shared.resources.setlist_service_label
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.DatePickerModal
import com.xperiencemusic.shared.ui.components.EditorTopBar
import com.xperiencemusic.shared.ui.components.SectionTitle
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SetlistEditorScreen(
    onBack: () -> Unit,
    onPublished: () -> Unit,
    viewModel: SetlistEditorViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isPublished) { if (state.isPublished) onPublished() }
    SetlistEditorScreenContent(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

private enum class EditedDate { REHEARSAL, SERVICE }

@Composable
fun SetlistEditorScreenContent(
    state: SetlistEditorUiState,
    onEvent: (SetlistEditorEvent) -> Unit,
    onBack: () -> Unit,
) {
    var editedDate by remember { mutableStateOf<EditedDate?>(null) }

    Scaffold(
        topBar = {
            EditorTopBar(
                title = stringResource(Res.string.setlist_editor_title),
                actionLabel = stringResource(Res.string.setlist_publish),
                isActionEnabled = state.canPublish,
                isWorking = state.isPublishing,
                onBack = onBack,
                onAction = { onEvent(SetlistEditorEvent.PublishClicked) },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).imePadding(),
            contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            state.error?.let { error ->
                item {
                    Text(
                        stringResource(
                            when (error) {
                                SetlistEditorError.FORBIDDEN -> Res.string.error_forbidden
                                SetlistEditorError.INVALID -> Res.string.setlist_error_invalid
                                SetlistEditorError.NETWORK -> Res.string.error_generic
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            item {
                AppCard {
                    SectionTitle(stringResource(Res.string.setlist_dates_title))
                    DateRow(stringResource(Res.string.setlist_rehearsal_label), state.rehearsalLabel) {
                        editedDate = EditedDate.REHEARSAL
                    }
                    DateRow(stringResource(Res.string.setlist_service_label), state.serviceLabel) {
                        editedDate = EditedDate.SERVICE
                    }
                    if (state.hasDateConflict) {
                        Text(
                            stringResource(Res.string.setlist_date_conflict),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.message,
                    onValueChange = { onEvent(SetlistEditorEvent.MessageChanged(it)) },
                    label = { Text(stringResource(Res.string.setlist_message_label)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { SectionTitle(stringResource(Res.string.setlist_selected_title, state.selected.size)) }
            if (state.selected.isEmpty()) {
                item {
                    Text(stringResource(Res.string.setlist_selected_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            itemsIndexed(state.selected, key = { _, song -> "selected-${song.id}" }) { index, song ->
                SelectedSongRow(
                    position = index + 1,
                    song = song,
                    isFirst = index == 0,
                    isLast = index == state.selected.lastIndex,
                    onEvent = onEvent,
                )
            }
            item { SectionTitle(stringResource(Res.string.setlist_add_title), Modifier.padding(top = Spacing.l)) }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { onEvent(SetlistEditorEvent.QueryChanged(it)) },
                    placeholder = { Text(stringResource(Res.string.library_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(state.available, key = { "available-${it.id}" }) { song ->
                AvailableSongRow(song, onAdd = { onEvent(SetlistEditorEvent.SongAdded(song.id)) })
            }
        }
    }

    editedDate?.let { which ->
        DatePickerModal(
            initialDate = if (which == EditedDate.REHEARSAL) state.rehearsalDate else state.serviceDate,
            onDatePicked = { date: LocalDate ->
                onEvent(
                    if (which == EditedDate.REHEARSAL) {
                        SetlistEditorEvent.RehearsalDatePicked(date)
                    } else {
                        SetlistEditorEvent.ServiceDatePicked(date)
                    },
                )
            },
            onDismiss = { editedDate = null },
        )
    }
}

@Composable
private fun DateRow(label: String, value: String, onChange: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = onChange) { Text(stringResource(Res.string.setlist_change_date)) }
    }
}

@Composable
private fun SelectedSongRow(
    position: Int,
    song: SetlistSongUi,
    isFirst: Boolean,
    isLast: Boolean,
    onEvent: (SetlistEditorEvent) -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$position",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = Spacing.m),
            )
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall)
                Text(song.keyLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = { onEvent(SetlistEditorEvent.SongMovedUp(song.id)) }, enabled = !isFirst) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(Res.string.setlist_move_up, song.title))
            }
            IconButton(onClick = { onEvent(SetlistEditorEvent.SongMovedDown(song.id)) }, enabled = !isLast) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(Res.string.setlist_move_down, song.title))
            }
            IconButton(onClick = { onEvent(SetlistEditorEvent.SongRemoved(song.id)) }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.setlist_remove, song.title))
            }
        }
    }
}

@Composable
private fun AvailableSongRow(song: SetlistSongUi, onAdd: () -> Unit) {
    AppCard(onClick = onAdd) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall)
                Text(song.keyLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(Res.string.setlist_add, song.title),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
private fun SetlistEditorScreenPreview() {
    AppTheme {
        SetlistEditorScreenContent(
            SetlistEditorUiState(
                rehearsalDate = LocalDate(2026, 10, 13),
                serviceDate = LocalDate(2026, 10, 18),
                rehearsalLabel = "mardi 13 oct.",
                serviceLabel = "dimanche 18 oct.",
                selected = listOf(SetlistSongUi("1", "À toi la gloire", "Ré majeur"), SetlistSongUi("2", "Amazing Grace", "Sol majeur")),
                available = listOf(SetlistSongUi("3", "Holy, Holy, Holy", "Ré majeur")),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
