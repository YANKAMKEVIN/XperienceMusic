package com.xperiencemusic.shared.ui.song

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.resources.voice_record_cta
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.music.chart.ChartLine
import com.xperiencemusic.music.chart.ChartSegment
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.VoicePart
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
import com.xperiencemusic.shared.resources.song_chart_title
import com.xperiencemusic.shared.resources.song_delete_confirm_cancel
import com.xperiencemusic.shared.resources.song_delete_confirm_ok
import com.xperiencemusic.shared.resources.song_delete_confirm_text
import com.xperiencemusic.shared.resources.song_delete_confirm_title
import com.xperiencemusic.shared.resources.song_not_found
import com.xperiencemusic.shared.resources.song_recordings_empty
import com.xperiencemusic.shared.resources.song_recordings_title
import com.xperiencemusic.shared.resources.stage_open
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.StageIcons
import com.xperiencemusic.shared.ui.components.SectionTitle
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.xperiencemusic.shared.ui.components.LoadingIndicator
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.resources.song_not_found_text

@Composable
fun SongDetailScreen(
    songId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddRecording: () -> Unit,
    onOpenStage: (transposeSemitones: Int, notation: Notation) -> Unit,
    onRecordVoice: () -> Unit = {},
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
        onRecordVoice = onRecordVoice,
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
    onRecordVoice: () -> Unit = {},
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
                title = {},
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
                state.isLoading -> LoadingIndicator()
                state.isMissing -> EmptyState(
                    icon = XmIcons.Book,
                    title = stringResource(Res.string.song_not_found),
                    text = stringResource(Res.string.song_not_found_text),
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = Spacing.l),
                )
                else -> SongDetailBody(state, onEvent, onAddRecording, onOpenStage, onRecordVoice)
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
    onRecordVoice: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        item { SongHero(state) }
        item { KeyAndNotation(state, onEvent) }
        item {
            GradientButton(onClick = onOpenStage, modifier = Modifier.fillMaxWidth()) {
                Icon(StageIcons.Fullscreen, contentDescription = null, modifier = Modifier.padding(end = Spacing.s))
                Text(stringResource(Res.string.stage_open), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (state.canRecordVoice) {
            item {
                OutlinedButton(onClick = onRecordVoice, modifier = Modifier.fillMaxWidth()) {
                    Icon(XmIcons.Mic, contentDescription = null, modifier = Modifier.padding(end = Spacing.s).size(Sizes.iconM))
                    Text(stringResource(Res.string.voice_record_cta))
                }
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
private fun SongHero(state: SongDetailUiState) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            state.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            listOfNotNull(state.keyHeading.ifEmpty { null }, state.artist).joinToString(" · "),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
                keyHeading = "Mi majeur",
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
