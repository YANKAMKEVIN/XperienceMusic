package com.xperiencemusic.shared.presentation.recordingform

import kotlinx.datetime.LocalDate
import com.xperiencemusic.shared.domain.usecase.UploadVoiceTrackUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.AudioFile
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.UploadRecordingUseCase
import com.xperiencemusic.shared.presentation.common.Formatting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecordingFormViewModel(
    private val songId: String,
    observeSong: ObserveSongUseCase,
    private val uploadRecording: UploadRecordingUseCase,
    getToday: GetTodayUseCase,
    private val uploadVoiceTrack: UploadVoiceTrackUseCase,
    /** Set when the take comes from a week's rehearsal: it fills the name and the date. */
    private val rehearsalDate: LocalDate? = null,
) : ViewModel() {
    private val _state = getToday().let { today ->
        // A rehearsal still to come cannot have been recorded yet.
        val recordedOn = rehearsalDate?.let { minOf(it, today) } ?: today
        MutableStateFlow(
            RecordingFormUiState(
                recordedOn = recordedOn,
                rehearsalDate = rehearsalDate,
            ),
        )
    }
    val state: StateFlow<RecordingFormUiState> = _state.asStateFlow()

    /** Kept out of the UI state: it can weigh tens of megabytes. */
    private var audio: AudioFile? = null

    init {
        viewModelScope.launch {
            val song = observeSong(songId).filterNotNull().first()
            _state.update { it.withSong(song, isRehearsal = rehearsalDate != null) }
        }
    }

    fun onEvent(event: RecordingFormEvent) {
        when (event) {
            is RecordingFormEvent.TitleChanged -> _state.update { it.copy(title = event.title, error = null) }
            is RecordingFormEvent.DefaultTitleResolved -> _state.update { if (it.title.isBlank()) it.copy(title = event.title) else it }
            is RecordingFormEvent.DatePicked -> _state.update { it.copy(recordedOn = event.date) }
            is RecordingFormEvent.FilePicked -> onFilePicked(event.file)
            is RecordingFormEvent.VoicePartSelected -> _state.update { it.copy(voicePart = event.voicePart, error = null) }
            is RecordingFormEvent.MakeReferenceChanged -> _state.update { it.copy(makeReference = event.makeReference) }
            is RecordingFormEvent.ReplaceSelected -> _state.update { it.copy(replaceId = event.recordingId, error = null) }
            RecordingFormEvent.SaveClicked -> save()
        }
    }

    private fun onFilePicked(file: AudioFile?) {
        if (file == null) {
            // Cancelled: keep any file chosen before.
            return
        }
        if (file.bytes.isEmpty()) {
            _state.update { it.copy(error = RecordingFormError.FILE_UNREADABLE) }
            return
        }
        audio = file
        _state.update {
            it.copy(
                file = PickedFileUi(
                    name = file.fileName,
                    sizeBytes = file.bytes.size.toLong(),
                    durationLabel = file.durationSeconds?.let(Formatting::duration),
                ),
                error = if (file.bytes.size > AudioFile.MAX_BYTES) RecordingFormError.FILE_TOO_LARGE else null,
            )
        }
    }

    private fun save() {
        val current = _state.value
        val file = audio
        if (!current.canSave || file == null) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val voice = current.voicePart
            val result = if (voice != null) {
                uploadVoiceTrack(songId, voice, file)
            } else {
                uploadRecording(
                    songId = songId,
                    title = current.title,
                    recordedOn = current.recordedOn,
                    audio = file,
                    makeReference = current.makeReference,
                    replace = current.replaceId.takeIf { current.isFull },
                )
            }
            when (result) {
                is DomainResult.Success -> _state.update { it.copy(isSaving = false, isSaved = true) }
                is DomainResult.Failure -> _state.update { it.copy(isSaving = false, error = result.error.toFormError()) }
            }
        }
    }
}

private fun RecordingFormUiState.withSong(song: Song, isRehearsal: Boolean) = copy(
    isLoading = false,
    songTitle = song.title,
    existing = song.recordings
        .sortedWith(compareByDescending<Recording> { it.isReference }.thenByDescending { it.recordedOn })
        .map { ExistingRecordingUi(it.id, it.title, it.recordedOn, it.isReference) },
    isFull = song.recordings.size >= Song.MAX_RECORDINGS,
    existingVoices = song.voiceTracks.map { it.voicePart }.toSet(),
    currentReferenceTitle = song.referenceRecording?.title,
    // The first recording of a song is always the reference; a rehearsal take is uploaded to be worked from.
    makeReference = song.recordings.isEmpty() || isRehearsal,
)

private fun DomainError.toFormError() = when (this) {
    DomainError.FORBIDDEN, DomainError.NOT_SIGNED_IN -> RecordingFormError.FORBIDDEN
    DomainError.FILE_TOO_LARGE -> RecordingFormError.FILE_TOO_LARGE
    DomainError.RECORDING_LIMIT_REACHED -> RecordingFormError.LIMIT_REACHED
    DomainError.INVALID_INPUT -> RecordingFormError.FILE_UNREADABLE
    else -> RecordingFormError.NETWORK
}
