package com.xperiencemusic.shared.ui.recordingform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.recordingform.ExistingRecordingUi
import com.xperiencemusic.shared.presentation.recordingform.PickedFileUi
import com.xperiencemusic.shared.presentation.recordingform.RecordingFormError
import com.xperiencemusic.shared.presentation.recordingform.RecordingFormEvent
import com.xperiencemusic.shared.presentation.recordingform.RecordingFormUiState
import com.xperiencemusic.shared.presentation.recordingform.RecordingFormViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.error_forbidden
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.recording_form_change_file
import com.xperiencemusic.shared.resources.recording_form_date_label
import com.xperiencemusic.shared.resources.recording_form_error_limit
import com.xperiencemusic.shared.resources.recording_form_error_too_large
import com.xperiencemusic.shared.resources.recording_form_error_unreadable
import com.xperiencemusic.shared.resources.recording_form_file_meta
import com.xperiencemusic.shared.resources.recording_form_file_title
import com.xperiencemusic.shared.resources.recording_form_full_help
import com.xperiencemusic.shared.resources.recording_form_full_title
import com.xperiencemusic.shared.resources.recording_form_name_hint
import com.xperiencemusic.shared.resources.recording_form_name_label
import com.xperiencemusic.shared.resources.recording_form_pick_file
import com.xperiencemusic.shared.resources.recording_form_reference
import com.xperiencemusic.shared.resources.recording_form_reference_help
import com.xperiencemusic.shared.resources.recording_form_save
import com.xperiencemusic.shared.resources.recording_form_song
import com.xperiencemusic.shared.resources.recording_form_title
import com.xperiencemusic.shared.resources.recording_form_uploading
import com.xperiencemusic.shared.resources.setlist_change_date
import com.xperiencemusic.shared.resources.song_recording_reference
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.DatePickerModal
import com.xperiencemusic.shared.ui.components.EditorTopBar
import com.xperiencemusic.shared.ui.components.SectionTitle
import com.xperiencemusic.shared.ui.platform.rememberAudioFilePicker
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RecordingFormScreen(
    songId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RecordingFormViewModel = koinViewModel(key = "recording-$songId") { parametersOf(songId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onSaved() }
    val pickFile = rememberAudioFilePicker { viewModel.onEvent(RecordingFormEvent.FilePicked(it)) }
    RecordingFormScreenContent(state = state, onEvent = viewModel::onEvent, onPickFile = pickFile, onBack = onBack)
}

@Composable
fun RecordingFormScreenContent(
    state: RecordingFormUiState,
    onEvent: (RecordingFormEvent) -> Unit,
    onPickFile: () -> Unit,
    onBack: () -> Unit,
) {
    var isPickingDate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            EditorTopBar(
                title = stringResource(Res.string.recording_form_title),
                actionLabel = stringResource(Res.string.recording_form_save),
                isActionEnabled = state.canSave,
                isWorking = state.isSaving,
                onBack = onBack,
                onAction = { onEvent(RecordingFormEvent.SaveClicked) },
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
            Text(
                stringResource(Res.string.recording_form_song, state.songTitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.isSaving) {
                Text(stringResource(Res.string.recording_form_uploading), color = MaterialTheme.colorScheme.secondary)
            }
            state.error?.let { ErrorText(it) }

            SectionTitle(stringResource(Res.string.recording_form_file_title))
            state.file?.let { FileCard(it) }
            OutlinedButton(onClick = onPickFile, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        if (state.file == null) Res.string.recording_form_pick_file else Res.string.recording_form_change_file,
                    ),
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = { onEvent(RecordingFormEvent.TitleChanged(it)) },
                label = { Text(stringResource(Res.string.recording_form_name_label)) },
                placeholder = { Text(stringResource(Res.string.recording_form_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.recording_form_date_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(state.dateLabel, style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = { isPickingDate = true }) { Text(stringResource(Res.string.setlist_change_date)) }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(
                    value = state.makeReference,
                    role = Role.Switch,
                    onValueChange = { onEvent(RecordingFormEvent.MakeReferenceChanged(it)) },
                ),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.recording_form_reference), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(Res.string.recording_form_reference_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.makeReference, onCheckedChange = null)
            }

            if (state.isFull) ReplaceChooser(state, onEvent)
        }
    }

    if (isPickingDate) {
        DatePickerModal(
            initialDate = state.recordedOn,
            onDatePicked = { onEvent(RecordingFormEvent.DatePicked(it)) },
            onDismiss = { isPickingDate = false },
        )
    }
}

@Composable
private fun ErrorText(error: RecordingFormError) {
    Text(
        stringResource(
            when (error) {
                RecordingFormError.FORBIDDEN -> Res.string.error_forbidden
                RecordingFormError.FILE_UNREADABLE -> Res.string.recording_form_error_unreadable
                RecordingFormError.FILE_TOO_LARGE -> Res.string.recording_form_error_too_large
                RecordingFormError.LIMIT_REACHED -> Res.string.recording_form_error_limit
                RecordingFormError.NETWORK -> Res.string.error_generic
            },
        ),
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun FileCard(file: PickedFileUi) {
    AppCard {
        Text(file.name, style = MaterialTheme.typography.titleSmall)
        Text(
            file.durationLabel?.let { stringResource(Res.string.recording_form_file_meta, it, file.sizeLabel) } ?: file.sizeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReplaceChooser(state: RecordingFormUiState, onEvent: (RecordingFormEvent) -> Unit) {
    AppCard {
        SectionTitle(stringResource(Res.string.recording_form_full_title, state.existing.size))
        Text(stringResource(Res.string.recording_form_full_help), style = MaterialTheme.typography.bodySmall)
        Column(Modifier.selectableGroup().padding(top = Spacing.s)) {
            state.existing.forEach { recording -> ReplaceOption(recording, state.replaceId == recording.id, onEvent) }
        }
    }
}

@Composable
private fun ReplaceOption(recording: ExistingRecordingUi, isSelected: Boolean, onEvent: (RecordingFormEvent) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = { onEvent(RecordingFormEvent.ReplaceSelected(recording.id)) },
            )
            .padding(vertical = Spacing.xs),
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Column(Modifier.weight(1f).padding(start = Spacing.s)) {
            Text(recording.title, style = MaterialTheme.typography.bodyLarge)
            Text(recording.dateLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (recording.isReference) {
            Icon(
                Icons.Filled.Star,
                contentDescription = stringResource(Res.string.song_recording_reference),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
private fun RecordingFormScreenFullPreview() {
    AppTheme {
        RecordingFormScreenContent(
            RecordingFormUiState(
                isLoading = false,
                songTitle = "À toi la gloire",
                title = "Répétition du mardi",
                recordedOn = LocalDate(2026, 10, 13),
                dateLabel = "13 oct. 2026",
                file = PickedFileUi("repet-13-10.m4a", "4,2 Mo", "3:32"),
                existing = listOf(
                    ExistingRecordingUi("1", "Prestation", "4 oct. 2026", isReference = true),
                    ExistingRecordingUi("2", "Acoustique", "20 sept. 2026", isReference = false),
                    ExistingRecordingUi("3", "Répétition", "1 sept. 2026", isReference = false),
                ),
                isFull = true,
                replaceId = "3",
            ),
            onEvent = {},
            onPickFile = {},
            onBack = {},
        )
    }
}
