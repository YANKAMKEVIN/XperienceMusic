package com.xperiencemusic.shared.presentation.songeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.music.chart.ChordChart
import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.SongDraft
import com.xperiencemusic.shared.domain.usecase.DeleteSongUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.SaveSongUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Creates a song when [songId] is null, edits it otherwise. */
class SongEditorViewModel(
    private val songId: String?,
    observeSong: ObserveSongUseCase,
    private val saveSong: SaveSongUseCase,
    private val deleteSong: DeleteSongUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SongEditorUiState(isLoading = songId != null, isNew = songId == null))
    val state: StateFlow<SongEditorUiState> = _state.asStateFlow()

    init {
        if (songId != null) {
            viewModelScope.launch {
                val song = observeSong(songId).filterNotNull().first()
                _state.update {
                    it.copy(isLoading = false, title = song.title, artist = song.artist.orEmpty(), key = song.key)
                        .withChart(song.chart)
                }
            }
        }
    }

    fun onEvent(event: SongEditorEvent) {
        when (event) {
            is SongEditorEvent.TitleChanged -> _state.update { it.copy(title = event.title, error = null) }
            is SongEditorEvent.ArtistChanged -> _state.update { it.copy(artist = event.artist) }
            is SongEditorEvent.TonicSelected -> _state.update { it.withKey(it.key.copy(tonic = event.tonic)) }
            is SongEditorEvent.ModeSelected -> _state.update { it.withKey(it.key.copy(mode = event.mode)) }
            is SongEditorEvent.ChartChanged -> _state.update { it.withChart(event.chart) }
            SongEditorEvent.SaveClicked -> save()
            SongEditorEvent.DeleteClicked -> _state.update { it.copy(isConfirmingDeletion = true) }
            SongEditorEvent.DeleteDismissed -> _state.update { it.copy(isConfirmingDeletion = false) }
            SongEditorEvent.DeleteConfirmed -> delete()
        }
    }

    private fun delete() {
        val id = songId ?: return
        _state.update { it.copy(isConfirmingDeletion = false, isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = deleteSong(id)) {
                is DomainResult.Success -> _state.update { it.copy(isSaving = false, isDeleted = true) }
                is DomainResult.Failure -> _state.update { it.copy(isSaving = false, error = result.error.toEditorError()) }
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (!current.canSave) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val draft = SongDraft(current.title, current.artist, current.key, current.chart)
            when (val result = saveSong(songId, draft)) {
                is DomainResult.Success -> _state.update { it.copy(isSaving = false, savedSongId = result.value) }
                is DomainResult.Failure -> _state.update { it.copy(isSaving = false, error = result.error.toEditorError()) }
            }
        }
    }
}

/**
 * The text is kept as typed: Nashville numbers then follow the new key, and chords typed as note
 * names keep their pitch (they are converted to numbers against the key chosen at save time).
 */
private fun SongEditorUiState.withKey(key: Key): SongEditorUiState = copy(key = key).withChart(chart)

private fun SongEditorUiState.withChart(chart: String): SongEditorUiState = copy(
    chart = chart,
    preview = ChordChart.render(chart, key, Notation.SOLFEGE),
    unknownChords = ChordChart.unknownChords(chart, key).distinct(),
)

private fun DomainError.toEditorError() = when (this) {
    DomainError.FORBIDDEN, DomainError.NOT_SIGNED_IN -> SongEditorError.FORBIDDEN
    DomainError.INVALID_INPUT -> SongEditorError.MISSING_TITLE
    else -> SongEditorError.NETWORK
}
