package com.xperiencemusic.shared.presentation.setlisteditor

import com.xperiencemusic.shared.presentation.library.LibrarySongUi
import kotlinx.datetime.LocalDate

data class SetlistEditorUiState(
    val rehearsalDate: LocalDate,
    val serviceDate: LocalDate,
    val message: String = "",
    val query: String = "",
    /** In setlist order. */
    val selected: List<SetlistSongUi> = emptyList(),
    /**
     * Not selected yet: without a search, the repertoire (songs of a setlist or with a recording),
     * by title; with one, the matches across the whole catalogue, lyrics included.
     */
    val available: List<LibrarySongUi> = emptyList(),
    val isPublishing: Boolean = false,
    val error: SetlistEditorError? = null,
    val isPublished: Boolean = false,
    val isEditing: Boolean = false,
    /** Another week already has a setlist on the chosen Sunday: publishing replaces it. */
    val replacesExisting: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    /** Leaving now would lose a change to the dates, the message or the songs. */
    val hasUnsavedChanges: Boolean = false,
    /** "Abandonner les modifications ?" is on screen. */
    val isConfirmingDiscard: Boolean = false,
    /** Set when the editor may close without publishing; the screen then navigates away. */
    val isLeaving: Boolean = false,
    /** Unusual dates: publishing stays possible, the admin is only asked to check. */
    val dateWarning: SetlistDateWarning? = null,
    /** "Nouveau chant" is open; null otherwise. */
    val newSong: NewSongUi? = null,
) {
    val canPublish: Boolean get() = !isPublishing && selected.isNotEmpty() && serviceDate >= rehearsalDate
    val hasDateConflict: Boolean get() = serviceDate < rehearsalDate
    val isSearching: Boolean get() = query.isNotBlank()
}

enum class SetlistDateWarning { PAST_REHEARSAL, LONG_GAP }

/**
 * A song missing from the library, typed as the WhatsApp message gave it: a title and its YouTube
 * video. Confirming creates it in the library and adds it to the week.
 */
data class NewSongUi(
    val title: String = "",
    val youtubeUrl: String = "",
    val isSaving: Boolean = false,
    val error: NewSongError? = null,
) {
    val canConfirm: Boolean get() = !isSaving && title.isNotBlank()
}

enum class NewSongError { MISSING_TITLE, INVALID_YOUTUBE_LINK, FORBIDDEN, NETWORK }

data class SetlistSongUi(val id: String, val title: String, val keyLabel: String)

enum class SetlistEditorError { FORBIDDEN, INVALID, NETWORK }

sealed interface SetlistEditorEvent {
    data class QueryChanged(val query: String) : SetlistEditorEvent
    data class SongAdded(val id: String) : SetlistEditorEvent
    data class SongRemoved(val id: String) : SetlistEditorEvent
    data class SongMovedUp(val id: String) : SetlistEditorEvent
    data class SongMovedDown(val id: String) : SetlistEditorEvent
    data class MessageChanged(val message: String) : SetlistEditorEvent
    data class RehearsalDatePicked(val date: LocalDate) : SetlistEditorEvent
    data class ServiceDatePicked(val date: LocalDate) : SetlistEditorEvent
    /** Opens "Nouveau chant", its title filled with what was searched. */
    data object NewSongClicked : SetlistEditorEvent
    data class NewSongTitleChanged(val title: String) : SetlistEditorEvent
    data class NewSongLinkChanged(val url: String) : SetlistEditorEvent
    data object NewSongConfirmed : SetlistEditorEvent
    data object NewSongDismissed : SetlistEditorEvent
    data object PublishClicked : SetlistEditorEvent
    data object DeleteConfirmed : SetlistEditorEvent
    /** The back arrow or the system back. */
    data object BackClicked : SetlistEditorEvent
    data object DiscardConfirmed : SetlistEditorEvent
    data object DiscardDismissed : SetlistEditorEvent
}
