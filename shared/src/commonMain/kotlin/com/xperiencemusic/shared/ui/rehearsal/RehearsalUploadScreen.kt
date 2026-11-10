package com.xperiencemusic.shared.ui.rehearsal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.rehearsal.RehearsalSongUi
import com.xperiencemusic.shared.presentation.rehearsal.RehearsalUploadUiState
import com.xperiencemusic.shared.presentation.rehearsal.RehearsalUploadViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.action_upload_rehearsal
import com.xperiencemusic.shared.resources.rehearsal_upload_add
import com.xperiencemusic.shared.resources.rehearsal_upload_date
import com.xperiencemusic.shared.resources.rehearsal_upload_empty
import com.xperiencemusic.shared.resources.rehearsal_upload_help
import com.xperiencemusic.shared.resources.rehearsal_upload_new
import com.xperiencemusic.shared.resources.rehearsal_upload_no_reference
import com.xperiencemusic.shared.resources.rehearsal_upload_reference
import com.xperiencemusic.shared.resources.rehearsal_upload_voices
import com.xperiencemusic.shared.ui.brand.KeyPill
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.LoadingIndicator
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.xperiencemusic.shared.ui.format.rememberFormattingWords

/** [onAddTake] opens the recording form on a song, with the rehearsal's date. */
@Composable
fun RehearsalUploadScreen(
    serviceDate: LocalDate,
    onBack: () -> Unit,
    onAddTake: (songId: String, rehearsalDate: LocalDate) -> Unit,
    viewModel: RehearsalUploadViewModel = koinViewModel(key = "rehearsal-$serviceDate") { parametersOf(serviceDate) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RehearsalUploadScreenContent(state, onBack, onAddTake)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RehearsalUploadScreenContent(
    state: RehearsalUploadUiState,
    onBack: () -> Unit,
    onAddTake: (songId: String, rehearsalDate: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.action_upload_rehearsal)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (state.isLoading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.l, top = Spacing.s, end = Spacing.l, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            val rehearsalDate = state.rehearsalDate
            if (rehearsalDate == null || state.songs.isEmpty()) {
                item { Text(stringResource(Res.string.rehearsal_upload_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                return@LazyColumn
            }
            item {
                Column {
                    Text(
                        stringResource(Res.string.rehearsal_upload_date, rememberFormattingWords().dayAndDate(rehearsalDate)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(Res.string.rehearsal_upload_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
            items(state.songs, key = { it.id }) { song -> SongRow(song, onClick = { onAddTake(song.id, rehearsalDate) }) }
        }
    }
}

@Composable
private fun SongRow(song: RehearsalSongUi, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    KeyPill(song.keyLabel)
                }
                Text(
                    song.referenceTitle?.let { stringResource(Res.string.rehearsal_upload_reference, it) }
                        ?: stringResource(Res.string.rehearsal_upload_no_reference),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
                if (song.voiceParts.isNotEmpty()) {
                    Text(
                        stringResource(Res.string.rehearsal_upload_voices, song.voiceParts.map { it.label() }.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (song.hasNewTake) {
                    Text(
                        stringResource(Res.string.rehearsal_upload_new),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(Res.string.rehearsal_upload_add, song.title),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
private fun RehearsalUploadScreenPreview() {
    AppTheme {
        RehearsalUploadScreenContent(
            RehearsalUploadUiState(
                isLoading = false,
                rehearsalDate = LocalDate(2026, 11, 10),
                songs = listOf(
                    RehearsalSongUi(
                        "a", "À toi la gloire", "Ré", "Répétition du mar. 10 nov.",
                        listOf(VoicePart.SOPRANO, VoicePart.ALTO), hasNewTake = true,
                    ),
                    RehearsalSongUi("b", "Holy, Holy, Holy", "Ré", null, emptyList(), hasNewTake = false),
                    RehearsalSongUi("c", "Amazing Grace", "Sol", "Version propre", emptyList(), hasNewTake = false),
                ),
            ),
            onBack = {}, onAddTake = { _, _ -> },
        )
    }
}
