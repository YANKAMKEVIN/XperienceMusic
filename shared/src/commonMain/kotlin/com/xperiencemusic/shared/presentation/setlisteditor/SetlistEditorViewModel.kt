package com.xperiencemusic.shared.presentation.setlisteditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.usecase.PublishSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import com.xperiencemusic.shared.domain.usecase.SuggestBandWeekUseCase
import com.xperiencemusic.shared.presentation.common.Formatting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Choices made on the screen; the song lists come from the library. */
data class SetlistDraft(
    val rehearsalDate: LocalDate,
    val serviceDate: LocalDate,
    val message: String = "",
    val query: String = "",
    val selectedIds: List<String> = emptyList(),
    val isPublishing: Boolean = false,
    val error: SetlistEditorError? = null,
    val isPublished: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SetlistEditorViewModel(
    private val searchSongs: SearchSongsUseCase,
    private val publishSetlist: PublishSetlistUseCase,
    suggestBandWeek: SuggestBandWeekUseCase,
) : ViewModel() {
    private val draft = MutableStateFlow(
        suggestBandWeek().let { SetlistDraft(rehearsalDate = it.rehearsalDate, serviceDate = it.serviceDate) },
    )

    /** The whole library, to resolve selected songs whatever the search. */
    private val library = searchSongs("")
    private val matches = draft.map { it.query }.flatMapLatest { searchSongs(it) }

    val state: StateFlow<SetlistEditorUiState> =
        combine(draft, library, matches, ::deriveSetlistEditorState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), deriveSetlistEditorState(draft.value, emptyList(), emptyList()))

    fun onEvent(event: SetlistEditorEvent) {
        when (event) {
            is SetlistEditorEvent.QueryChanged -> draft.update { it.copy(query = event.query) }
            is SetlistEditorEvent.SongAdded -> draft.update {
                if (event.id in it.selectedIds) it else it.copy(selectedIds = it.selectedIds + event.id, query = "")
            }
            is SetlistEditorEvent.SongRemoved -> draft.update { it.copy(selectedIds = it.selectedIds - event.id) }
            is SetlistEditorEvent.SongMovedUp -> draft.update { it.copy(selectedIds = it.selectedIds.moved(event.id, -1)) }
            is SetlistEditorEvent.SongMovedDown -> draft.update { it.copy(selectedIds = it.selectedIds.moved(event.id, +1)) }
            is SetlistEditorEvent.MessageChanged -> draft.update { it.copy(message = event.message) }
            is SetlistEditorEvent.RehearsalDatePicked -> draft.update { it.copy(rehearsalDate = event.date, error = null) }
            is SetlistEditorEvent.ServiceDatePicked -> draft.update { it.copy(serviceDate = event.date, error = null) }
            SetlistEditorEvent.PublishClicked -> publish()
        }
    }

    private fun publish() {
        val current = state.value
        if (!current.canPublish) return
        draft.update { it.copy(isPublishing = true, error = null) }
        viewModelScope.launch {
            val setlist = Setlist(
                id = "",
                rehearsalDate = current.rehearsalDate,
                serviceDate = current.serviceDate,
                songIds = current.selected.map { it.id },
                message = current.message.trim().ifEmpty { null },
                publishedBy = "",
            )
            when (val result = publishSetlist(setlist)) {
                is DomainResult.Success -> draft.update { it.copy(isPublishing = false, isPublished = true) }
                is DomainResult.Failure -> draft.update { it.copy(isPublishing = false, error = result.error.toEditorError()) }
            }
        }
    }
}

fun deriveSetlistEditorState(draft: SetlistDraft, library: List<Song>, matches: List<Song>): SetlistEditorUiState {
    val byId = library.associateBy { it.id }
    return SetlistEditorUiState(
        rehearsalDate = draft.rehearsalDate,
        serviceDate = draft.serviceDate,
        rehearsalLabel = Formatting.dayAndDate(draft.rehearsalDate),
        serviceLabel = Formatting.dayAndDate(draft.serviceDate),
        message = draft.message,
        query = draft.query,
        selected = draft.selectedIds.mapNotNull { byId[it]?.toSetlistSongUi() },
        available = matches.filterNot { it.id in draft.selectedIds }.map { it.toSetlistSongUi() },
        isPublishing = draft.isPublishing,
        error = draft.error,
        isPublished = draft.isPublished,
    )
}

private fun Song.toSetlistSongUi() = SetlistSongUi(id, title, key.label(Notation.SOLFEGE))

private fun List<String>.moved(id: String, offset: Int): List<String> {
    val from = indexOf(id)
    val to = from + offset
    if (from < 0 || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

private fun DomainError.toEditorError() = when (this) {
    DomainError.FORBIDDEN, DomainError.NOT_SIGNED_IN -> SetlistEditorError.FORBIDDEN
    DomainError.INVALID_INPUT -> SetlistEditorError.INVALID
    else -> SetlistEditorError.NETWORK
}
