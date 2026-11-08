package com.xperiencemusic.shared.presentation.setlisteditor

import kotlinx.datetime.daysUntil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.BandWeek
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.usecase.DeleteSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import com.xperiencemusic.shared.domain.usecase.PublishSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import com.xperiencemusic.shared.domain.usecase.SuggestBandWeekUseCase
import com.xperiencemusic.shared.presentation.common.Formatting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    /** The service date of the published setlist being edited; null for a new week. */
    val editedServiceDate: LocalDate? = null,
    /** Sundays that already have a setlist. */
    val publishedDates: Set<LocalDate> = emptySet(),
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    /** The choices once loaded, before any edit; null while loading. */
    val original: SetlistChoices? = null,
    val isConfirmingDiscard: Boolean = false,
    val isLeaving: Boolean = false,
    val today: LocalDate? = null,
) {
    val choices: SetlistChoices get() = SetlistChoices(rehearsalDate, serviceDate, message, selectedIds)
}

/** What publishing would send; the search is left out. */
data class SetlistChoices(
    val rehearsalDate: LocalDate,
    val serviceDate: LocalDate,
    val message: String,
    val selectedIds: List<String>,
)

@OptIn(ExperimentalCoroutinesApi::class)
/**
 * Edits the setlist of [serviceDate] when given, otherwise prepares the first coming week that has
 * none yet, so several weeks can be sent ahead.
 */
class SetlistEditorViewModel(
    private val searchSongs: SearchSongsUseCase,
    private val publishSetlist: PublishSetlistUseCase,
    private val deleteSetlist: DeleteSetlistUseCase,
    suggestBandWeek: SuggestBandWeekUseCase,
    observeSchedule: ObserveSetlistScheduleUseCase,
    serviceDate: LocalDate?,
) : ViewModel() {
    private val draft = MutableStateFlow(
        suggestBandWeek().let { SetlistDraft(rehearsalDate = it.rehearsalDate, serviceDate = it.serviceDate, today = suggestBandWeek.today()) },
    )

    init {
        viewModelScope.launch {
            val published = observeSchedule().first().weeks.map { it.setlist }
            val dates = published.map { it.serviceDate }.toSet()
            val edited = published.firstOrNull { it.serviceDate == serviceDate }
            draft.update { current ->
                if (edited != null) {
                    current.copy(
                        rehearsalDate = edited.rehearsalDate,
                        serviceDate = edited.serviceDate,
                        message = edited.message.orEmpty(),
                        selectedIds = edited.songIds,
                        editedServiceDate = edited.serviceDate,
                        publishedDates = dates,
                    )
                } else {
                    var week = BandWeek(current.rehearsalDate, current.serviceDate)
                    while (week.serviceDate in dates) week = week.next()
                    current.copy(rehearsalDate = week.rehearsalDate, serviceDate = week.serviceDate, publishedDates = dates)
                }.let { loaded -> loaded.copy(original = loaded.choices) }
            }
        }
    }

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
            SetlistEditorEvent.DeleteConfirmed -> delete()
            SetlistEditorEvent.BackClicked -> draft.update {
                if (it.hasUnsavedChanges) it.copy(isConfirmingDiscard = true) else it.copy(isLeaving = true)
            }
            SetlistEditorEvent.DiscardConfirmed -> draft.update { it.copy(isConfirmingDiscard = false, isLeaving = true) }
            SetlistEditorEvent.DiscardDismissed -> draft.update { it.copy(isConfirmingDiscard = false) }
        }
    }

    private fun delete() {
        val date = draft.value.editedServiceDate ?: return
        if (draft.value.isDeleting) return
        draft.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            when (val result = deleteSetlist(date)) {
                is DomainResult.Success -> draft.update { it.copy(isDeleting = false, isDeleted = true) }
                is DomainResult.Failure -> draft.update { it.copy(isDeleting = false, error = result.error.toEditorError()) }
            }
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
            val moved = draft.value.editedServiceDate?.takeIf { it != setlist.serviceDate }
            when (val result = publishSetlist(setlist)) {
                is DomainResult.Success -> {
                    // The week was moved to another Sunday: the old date no longer has it.
                    moved?.let { deleteSetlist(it) }
                    draft.update { it.copy(isPublishing = false, isPublished = true) }
                }
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
        isEditing = draft.editedServiceDate != null,
        replacesExisting = draft.serviceDate in draft.publishedDates && draft.serviceDate != draft.editedServiceDate,
        isDeleting = draft.isDeleting,
        isDeleted = draft.isDeleted,
        hasUnsavedChanges = draft.hasUnsavedChanges,
        isConfirmingDiscard = draft.isConfirmingDiscard,
        isLeaving = draft.isLeaving,
        dateWarning = draft.dateWarning(),
    )
}

/** A past date already published is left alone: only a date the admin just picked is questioned. */
private fun SetlistDraft.dateWarning(): SetlistDateWarning? = when {
    serviceDate < rehearsalDate -> null
    today != null && rehearsalDate < today && rehearsalDate != original?.rehearsalDate -> SetlistDateWarning.PAST_REHEARSAL
    rehearsalDate.daysUntil(serviceDate) > MAX_DAYS_BEFORE_SERVICE -> SetlistDateWarning.LONG_GAP
    else -> null
}

private const val MAX_DAYS_BEFORE_SERVICE = 21

/** Not once published or deleted: the week then left the editor on purpose. */
val SetlistDraft.hasUnsavedChanges: Boolean
    get() = original != null && !isLeaving && !isPublished && !isDeleted && choices != original

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
