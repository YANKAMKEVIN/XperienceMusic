package com.xperiencemusic.shared.ui.song

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.music.chart.ChartLine
import com.xperiencemusic.music.chart.ChartSegment
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.song.LinkUi
import com.xperiencemusic.shared.presentation.song.RecordingUi
import com.xperiencemusic.shared.presentation.song.SongDetailEvent
import com.xperiencemusic.shared.presentation.song.SongDetailUiState
import com.xperiencemusic.shared.presentation.song.SongDetailViewModel
import com.xperiencemusic.shared.presentation.song.SongMessage
import com.xperiencemusic.shared.presentation.song.VoiceTrackUi
import com.xperiencemusic.shared.presentation.common.PlaybackStatus
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_add_recording
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.action_edit_song
import com.xperiencemusic.shared.resources.error_forbidden
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.notation_letters
import com.xperiencemusic.shared.resources.notation_nashville
import com.xperiencemusic.shared.resources.notation_solfege
import com.xperiencemusic.shared.resources.song_chart_title
import com.xperiencemusic.shared.resources.song_delete_confirm_cancel
import com.xperiencemusic.shared.resources.song_delete_confirm_ok
import com.xperiencemusic.shared.resources.song_delete_confirm_text
import com.xperiencemusic.shared.resources.song_delete_confirm_title
import com.xperiencemusic.shared.resources.song_key
import com.xperiencemusic.shared.resources.song_links_title
import com.xperiencemusic.shared.resources.song_not_found
import com.xperiencemusic.shared.resources.song_original_key
import com.xperiencemusic.shared.resources.song_recording_actions
import com.xperiencemusic.shared.resources.song_recording_delete
import com.xperiencemusic.shared.resources.song_recording_meta
import com.xperiencemusic.shared.resources.song_recording_offline
import com.xperiencemusic.shared.resources.song_recording_reference
import com.xperiencemusic.shared.resources.song_recording_set_reference
import com.xperiencemusic.shared.resources.song_recordings_empty
import com.xperiencemusic.shared.resources.song_recordings_title
import com.xperiencemusic.shared.resources.song_transpose_down
import com.xperiencemusic.shared.resources.song_transpose_reset
import com.xperiencemusic.shared.resources.song_transpose_up
import com.xperiencemusic.shared.resources.song_voice_tracks_title
import com.xperiencemusic.shared.resources.stage_open
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.PlayPauseButton
import com.xperiencemusic.shared.ui.components.StageIcons
import com.xperiencemusic.shared.ui.components.SectionTitle
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SongDetailScreen(
    songId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddRecording: () -> Unit,
    onOpenStage: (transposeSemitones: Int, notation: Notation) -> Unit,
    viewModel: SongDetailViewModel = koinViewModel(key = songId) { parametersOf(songId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SongDetailScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onEdit = onEdit,
        onAddRecording = onAddRecording,
        onOpenStage = { onOpenStage(state.transposeSemitones, state.notation) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreenContent(
    state: SongDetailUiState,
    onEvent: (SongDetailEvent) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddRecording: () -> Unit,
    onOpenStage: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            getString(if (message == SongMessage.FORBIDDEN) Res.string.error_forbidden else Res.string.error_generic),
        )
        onEvent(SongDetailEvent.MessageShown)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                actions = {
                    if (state.canEditSong) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(Res.string.action_edit_song))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                // The app scaffold already applies the system bar insets.
                windowInsets = WindowInsets(0),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.isMissing -> Text(stringResource(Res.string.song_not_found), Modifier.align(Alignment.Center))
                else -> SongDetailBody(state, onEvent, onAddRecording, onOpenStage)
            }
        }
    }

    state.pendingDeletion?.let { recording ->
        AlertDialog(
            onDismissRequest = { onEvent(SongDetailEvent.DeleteDismissed) },
            title = { Text(stringResource(Res.string.song_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.song_delete_confirm_text, recording.title)) },
            confirmButton = {
                TextButton(onClick = { onEvent(SongDetailEvent.DeleteConfirmed) }) {
                    Text(stringResource(Res.string.song_delete_confirm_ok), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SongDetailEvent.DeleteDismissed) }) {
                    Text(stringResource(Res.string.song_delete_confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun SongDetailBody(
    state: SongDetailUiState,
    onEvent: (SongDetailEvent) -> Unit,
    onAddRecording: () -> Unit,
    onOpenStage: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        state.artist?.let { artist ->
            item { Text(artist, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item { KeyAndNotation(state, onEvent) }
        item {
            OutlinedButton(onClick = onOpenStage, modifier = Modifier.fillMaxWidth()) {
                Icon(StageIcons.Fullscreen, contentDescription = null, modifier = Modifier.padding(end = Spacing.s))
                Text(stringResource(Res.string.stage_open))
            }
        }
        item {
            AppCard {
                SectionTitle(stringResource(Res.string.song_chart_title), Modifier.padding(bottom = Spacing.s))
                ChordChartView(state.chart)
            }
        }
        item {
            SectionTitle(stringResource(Res.string.song_recordings_title, state.recordings.size, state.maxRecordings))
        }
        if (state.recordings.isEmpty()) {
            item {
                Text(stringResource(Res.string.song_recordings_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(state.recordings, key = { it.id }) { recording ->
            RecordingRow(recording, state.canManageRecordings, onEvent)
        }
        if (state.canManageRecordings) {
            item {
                OutlinedButton(onClick = onAddRecording, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = Spacing.s))
                    Text(stringResource(Res.string.action_add_recording))
                }
            }
        }
        if (state.voiceTracks.isNotEmpty()) {
            item { VoiceTracks(state.voiceTracks, onEvent) }
        }
        if (state.links.isNotEmpty()) {
            item { Links(state.links) }
        }
    }
}

@Composable
private fun KeyAndNotation(state: SongDetailUiState, onEvent: (SongDetailEvent) -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.song_key, state.keyLabel), style = MaterialTheme.typography.titleMedium)
                state.originalKeyLabel?.let {
                    Text(
                        stringResource(Res.string.song_original_key, it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { onEvent(SongDetailEvent.TransposeDownClicked) }) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(Res.string.song_transpose_down))
            }
            IconButton(onClick = { onEvent(SongDetailEvent.TransposeUpClicked) }) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(Res.string.song_transpose_up))
            }
            IconButton(
                onClick = { onEvent(SongDetailEvent.TransposeResetClicked) },
                enabled = state.originalKeyLabel != null,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(Res.string.song_transpose_reset))
            }
        }
        val notations = listOf(
            Notation.NASHVILLE to Res.string.notation_nashville,
            Notation.SOLFEGE to Res.string.notation_solfege,
            Notation.LETTERS to Res.string.notation_letters,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = Spacing.m)) {
            notations.forEachIndexed { index, (notation, label) ->
                SegmentedButton(
                    selected = state.notation == notation,
                    onClick = { onEvent(SongDetailEvent.NotationSelected(notation)) },
                    shape = SegmentedButtonDefaults.itemShape(index, notations.size),
                    label = { Text(stringResource(label), maxLines = 1) },
                )
            }
        }
    }
}

@Composable
private fun RecordingRow(recording: RecordingUi, canManage: Boolean, onEvent: (SongDetailEvent) -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayPauseButton(
                status = recording.playback,
                itemLabel = recording.title,
                onClick = { onEvent(SongDetailEvent.PlayRecordingClicked(recording.id)) },
            )
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (recording.isReference) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = stringResource(Res.string.song_recording_reference),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = Spacing.xs).size(16.dp),
                        )
                    }
                    Text(recording.title, style = MaterialTheme.typography.titleSmall)
                }
                val meta = stringResource(Res.string.song_recording_meta, recording.date, recording.duration)
                val offlineLabel = stringResource(Res.string.song_recording_offline)
                Text(
                    if (recording.isAvailableOffline) "$meta · $offlineLabel" else meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canManage) RecordingMenu(recording, onEvent)
        }
    }
}

@Composable
private fun RecordingMenu(recording: RecordingUi, onEvent: (SongDetailEvent) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.song_recording_actions))
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            if (!recording.isReference) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.song_recording_set_reference)) },
                    onClick = {
                        isExpanded = false
                        onEvent(SongDetailEvent.SetReferenceClicked(recording.id))
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.song_recording_delete), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    isExpanded = false
                    onEvent(SongDetailEvent.DeleteClicked(recording.id))
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceTracks(voiceTracks: List<VoiceTrackUi>, onEvent: (SongDetailEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionTitle(stringResource(Res.string.song_voice_tracks_title))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            voiceTracks.forEach { track ->
                val label = track.voicePart.label()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayPauseButton(
                        status = track.playback,
                        itemLabel = label,
                        onClick = { onEvent(SongDetailEvent.PlayVoiceTrackClicked(track.id, label)) },
                    )
                    Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = Spacing.xs))
                }
            }
        }
    }
}

@Composable
private fun Links(links: List<LinkUi>) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionTitle(stringResource(Res.string.song_links_title))
        links.forEach { link ->
            TextButton(onClick = { uriHandler.openUri(link.url) }) { Text(link.label) }
        }
    }
}

@Preview
@Composable
private fun SongDetailScreenPreview() {
    AppTheme {
        SongDetailScreenContent(
            SongDetailUiState(
                isLoading = false,
                title = "À toi la gloire",
                artist = "E. Budry",
                keyLabel = "Mi majeur",
                originalKeyLabel = "Ré majeur",
                notation = Notation.SOLFEGE,
                chart = listOf(
                    ChartLine.Section("Refrain"),
                    ChartLine.Lyrics(listOf(ChartSegment("Mi", "À toi la "), ChartSegment("Si", "gloire"))),
                ),
                recordings = listOf(
                    RecordingUi("1", "Répétition du mardi", "14 mai 2026", "3:32", isReference = true, playback = PlaybackStatus.PLAYING),
                    RecordingUi("2", "Version acoustique", "2 août 2026", "3:18", isReference = false),
                ),
                maxRecordings = 3,
                canManageRecordings = true,
                canEditSong = true,
                voiceTracks = listOf(VoiceTrackUi("s", VoicePart.SOPRANO), VoiceTrackUi("a", VoicePart.ALTO)),
            ),
            onEvent = {},
            onBack = {},
            onEdit = {},
            onAddRecording = {},
            onOpenStage = {},
        )
    }
}

@Preview
@Composable
private fun SongDetailScreenMissingPreview() {
    AppTheme { SongDetailScreenContent(SongDetailUiState(isLoading = false, isMissing = true), onEvent = {}, onBack = {}, onEdit = {}, onAddRecording = {}, onOpenStage = {}) }
}
