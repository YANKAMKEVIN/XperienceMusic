package com.xperiencemusic.shared.presentation.song

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.music.theory.SEMITONES_PER_OCTAVE
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.usecase.DeleteRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.SetReferenceRecordingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SongDetailViewModel(
    private val songId: String,
    observeSong: ObserveSongUseCase,
    observeCurrentMember: ObserveCurrentMemberUseCase,
    private val setReferenceRecording: SetReferenceRecordingUseCase,
    private val deleteRecording: DeleteRecordingUseCase,
) : ViewModel() {
    private val local = MutableStateFlow(SongDetailLocalState())

    val state: StateFlow<SongDetailUiState> =
        combine(observeSong(songId), observeCurrentMember(), local, ::deriveSongDetailState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SongDetailUiState())

    fun onEvent(event: SongDetailEvent) {
        when (event) {
            SongDetailEvent.TransposeUpClicked -> transpose(+1)
            SongDetailEvent.TransposeDownClicked -> transpose(-1)
            SongDetailEvent.TransposeResetClicked -> local.update { it.copy(transposeSemitones = 0) }
            is SongDetailEvent.NotationSelected -> local.update { it.copy(notation = event.notation) }
            is SongDetailEvent.SetReferenceClicked -> launchAction { setReferenceRecording(songId, event.recordingId) }
            is SongDetailEvent.DeleteClicked -> local.update { it.copy(pendingDeletionId = event.recordingId) }
            SongDetailEvent.DeleteDismissed -> local.update { it.copy(pendingDeletionId = null) }
            SongDetailEvent.DeleteConfirmed -> confirmDeletion()
            SongDetailEvent.MessageShown -> local.update { it.copy(message = null) }
        }
    }

    /** Keeps the offset within one octave either way: +7 and -5 land on the same key. */
    private fun transpose(step: Int) {
        local.update {
            val next = (it.transposeSemitones + step).mod(SEMITONES_PER_OCTAVE)
            it.copy(transposeSemitones = if (next > SEMITONES_PER_OCTAVE / 2) next - SEMITONES_PER_OCTAVE else next)
        }
    }

    private fun confirmDeletion() {
        val recordingId = local.value.pendingDeletionId ?: return
        local.update { it.copy(pendingDeletionId = null) }
        launchAction { deleteRecording(songId, recordingId) }
    }

    private fun launchAction(action: suspend () -> DomainResult<*>) {
        viewModelScope.launch {
            val result = action()
            if (result is DomainResult.Failure) {
                val message = if (result.error == DomainError.FORBIDDEN) SongMessage.FORBIDDEN else SongMessage.GENERIC
                local.update { it.copy(message = message) }
            }
        }
    }
}
