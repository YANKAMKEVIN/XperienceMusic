package com.xperiencemusic.shared.ui.songeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.music.chart.ChordChart
import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.presentation.songeditor.SongEditorError
import com.xperiencemusic.shared.presentation.songeditor.SongEditorEvent
import com.xperiencemusic.shared.presentation.songeditor.SongEditorUiState
import com.xperiencemusic.shared.presentation.songeditor.SongEditorViewModel
import com.xperiencemusic.shared.presentation.songeditor.SongImportError
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.editor_artist_label
import com.xperiencemusic.shared.resources.editor_chart_help
import com.xperiencemusic.shared.resources.editor_chart_label
import com.xperiencemusic.shared.resources.editor_chart_title
import com.xperiencemusic.shared.resources.editor_delete
import com.xperiencemusic.shared.resources.editor_delete_text
import com.xperiencemusic.shared.resources.editor_delete_title
import com.xperiencemusic.shared.resources.editor_edit_title
import com.xperiencemusic.shared.resources.editor_error_title
import com.xperiencemusic.shared.resources.editor_import_button
import com.xperiencemusic.shared.resources.editor_import_help
import com.xperiencemusic.shared.resources.editor_import_link
import com.xperiencemusic.shared.resources.editor_import_network
import com.xperiencemusic.shared.resources.editor_import_not_a_link
import com.xperiencemusic.shared.resources.editor_import_not_found
import com.xperiencemusic.shared.resources.editor_import_title
import com.xperiencemusic.shared.resources.editor_key_title
import com.xperiencemusic.shared.resources.editor_new_title
import com.xperiencemusic.shared.resources.editor_preview_title
import com.xperiencemusic.shared.resources.editor_save
import com.xperiencemusic.shared.resources.editor_title_label
import com.xperiencemusic.shared.resources.editor_unknown_chords
import com.xperiencemusic.shared.resources.error_forbidden
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.song_delete_confirm_cancel
import com.xperiencemusic.shared.resources.song_delete_confirm_ok
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.EditorTopBar
import com.xperiencemusic.shared.ui.components.KeyPicker
import com.xperiencemusic.shared.ui.components.SectionTitle
import com.xperiencemusic.shared.ui.song.ChordChartView
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** [songId] null creates a new song. */
@Composable
fun SongEditorScreen(
    songId: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: SongEditorViewModel = koinViewModel(key = "editor-$songId") { parametersOf(songId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedSongId) { state.savedSongId?.let(onSaved) }
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onDeleted() }
    SongEditorScreenContent(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
fun SongEditorScreenContent(
    state: SongEditorUiState,
    onEvent: (SongEditorEvent) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            EditorTopBar(
                title = stringResource(if (state.isNew) Res.string.editor_new_title else Res.string.editor_edit_title),
                actionLabel = stringResource(Res.string.editor_save),
                isActionEnabled = state.canSave,
                isWorking = state.isSaving,
                onBack = onBack,
                onAction = { onEvent(SongEditorEvent.SaveClicked) },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.l, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            state.error?.let { error ->
                Text(
                    stringResource(
                        when (error) {
                            SongEditorError.FORBIDDEN -> Res.string.error_forbidden
                            SongEditorError.MISSING_TITLE -> Res.string.editor_error_title
                            SongEditorError.NETWORK -> Res.string.error_generic
                        },
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.isNew) ImportFromLink(state, onEvent)
            OutlinedTextField(
                value = state.title,
                onValueChange = { onEvent(SongEditorEvent.TitleChanged(it)) },
                label = { Text(stringResource(Res.string.editor_title_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.artist,
                onValueChange = { onEvent(SongEditorEvent.ArtistChanged(it)) },
                label = { Text(stringResource(Res.string.editor_artist_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(stringResource(Res.string.editor_key_title))
            KeyPicker(
                key = state.key,
                onTonicSelected = { onEvent(SongEditorEvent.TonicSelected(it)) },
                onModeSelected = { onEvent(SongEditorEvent.ModeSelected(it)) },
            )

            SectionTitle(stringResource(Res.string.editor_chart_title))
            Text(
                stringResource(Res.string.editor_chart_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.chart,
                onValueChange = { onEvent(SongEditorEvent.ChartChanged(it)) },
                label = { Text(stringResource(Res.string.editor_chart_label)) },
                minLines = 8,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.unknownChords.isNotEmpty()) {
                Text(
                    stringResource(Res.string.editor_unknown_chords, state.unknownChords.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (state.preview.isNotEmpty() && state.chart.isNotBlank()) {
                AppCard {
                    SectionTitle(stringResource(Res.string.editor_preview_title), Modifier.padding(bottom = Spacing.s))
                    ChordChartView(state.preview)
                }
            }
            if (!state.isNew) {
                TextButton(
                    onClick = { onEvent(SongEditorEvent.DeleteClicked) },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.l),
                ) {
                    Text(stringResource(Res.string.editor_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (state.isConfirmingDeletion) {
        AlertDialog(
            onDismissRequest = { onEvent(SongEditorEvent.DeleteDismissed) },
            title = { Text(stringResource(Res.string.editor_delete_title, state.title)) },
            text = { Text(stringResource(Res.string.editor_delete_text)) },
            confirmButton = {
                TextButton(onClick = { onEvent(SongEditorEvent.DeleteConfirmed) }) {
                    Text(stringResource(Res.string.song_delete_confirm_ok), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SongEditorEvent.DeleteDismissed) }) {
                    Text(stringResource(Res.string.song_delete_confirm_cancel))
                }
            },
        )
    }
}

/** Fills the new song from one accords.app page. */
@Composable
private fun ImportFromLink(state: SongEditorUiState, onEvent: (SongEditorEvent) -> Unit) {
    AppCard {
        SectionTitle(stringResource(Res.string.editor_import_title))
        Text(
            stringResource(Res.string.editor_import_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        OutlinedTextField(
            value = state.importLink,
            onValueChange = { onEvent(SongEditorEvent.ImportLinkChanged(it)) },
            label = { Text(stringResource(Res.string.editor_import_link)) },
            singleLine = true,
            enabled = !state.isImporting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onEvent(SongEditorEvent.ImportClicked) }),
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
        )
        state.importError?.let { error ->
            Text(
                stringResource(
                    when (error) {
                        SongImportError.NOT_A_SONG_LINK -> Res.string.editor_import_not_a_link
                        SongImportError.NOT_FOUND -> Res.string.editor_import_not_found
                        SongImportError.NETWORK -> Res.string.editor_import_network
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
        GradientButton(
            onClick = { onEvent(SongEditorEvent.ImportClicked) },
            enabled = state.canImport,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
        ) {
            if (state.isImporting) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(Res.string.editor_import_button), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Preview
@Composable
private fun SongEditorScreenPreview() {
    val key = Key(7)
    val chart = "# Refrain\n[Sol]Saint, saint, [Do]saint [Dp]"
    AppTheme {
        SongEditorScreenContent(
            SongEditorUiState(
                title = "Nouveau chant",
                key = key,
                chart = chart,
                preview = ChordChart.render(chart, key, Notation.SOLFEGE),
                unknownChords = listOf("Dp"),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
