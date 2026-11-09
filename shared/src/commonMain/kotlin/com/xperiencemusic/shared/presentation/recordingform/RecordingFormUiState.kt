package com.xperiencemusic.shared.presentation.recordingform

import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.AudioFile
import kotlinx.datetime.LocalDate

data class RecordingFormUiState(
    val isLoading: Boolean = true,
    val songTitle: String = "",
    val title: String = "",
    val recordedOn: LocalDate,
    val dateLabel: String,
    val file: PickedFileUi? = null,
    val makeReference: Boolean = false,
    /** Who the take is for; null is the whole band, a voice stores it as that voice's track. */
    val voicePart: VoicePart? = null,
    val existingVoices: Set<VoicePart> = emptySet(),
    val currentReferenceTitle: String? = null,
    /** The song's current recordings; when it already has the maximum, one must be replaced. */
    val existing: List<ExistingRecordingUi> = emptyList(),
    val isFull: Boolean = false,
    val replaceId: String? = null,
    val isSaving: Boolean = false,
    val error: RecordingFormError? = null,
    val isSaved: Boolean = false,
) {
    val canSave: Boolean
        get() = !isLoading && !isSaving && file != null && (voicePart != null || (title.isNotBlank() && (!isFull || replaceId != null)))

    val replacesVoice: Boolean get() = voicePart != null && voicePart in existingVoices
}

data class PickedFileUi(val name: String, val sizeLabel: String, val durationLabel: String?)

data class ExistingRecordingUi(val id: String, val title: String, val dateLabel: String, val isReference: Boolean)

enum class RecordingFormError { FORBIDDEN, FILE_UNREADABLE, FILE_TOO_LARGE, LIMIT_REACHED, NETWORK }

sealed interface RecordingFormEvent {
    data class TitleChanged(val title: String) : RecordingFormEvent
    data class DatePicked(val date: LocalDate) : RecordingFormEvent
    data class FilePicked(val file: AudioFile?) : RecordingFormEvent
    data class VoicePartSelected(val voicePart: VoicePart?) : RecordingFormEvent
    data class MakeReferenceChanged(val makeReference: Boolean) : RecordingFormEvent
    data class ReplaceSelected(val recordingId: String) : RecordingFormEvent
    data object SaveClicked : RecordingFormEvent
}
