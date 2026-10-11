package com.xperiencemusic.shared.presentation.setlisteditor

import kotlinx.datetime.LocalDate

data class SetlistEditorUiState(
    val rehearsalDate: LocalDate,
    val serviceDate: LocalDate,
    val rehearsalLabel: String,
    val serviceLabel: String,
    val message: String = "",
    val query: String = "",
    /** In setlist order. */
    val selected: List<SetlistSongUi> = emptyList(),
    /** Songs matching [query] that are not selected yet. */
    val available: List<SetlistSongUi> = emptyList(),
    val isPublishing: Boolean = false,
    val error: SetlistEditorError? = null,
    val isPublished: Boolean = false,
    val isEditing: Boolean = false,
) {
    val canPublish: Boolean get() = !isPublishing && selected.isNotEmpty() && serviceDate >= rehearsalDate
    val hasDateConflict: Boolean get() = serviceDate < rehearsalDate
}

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
    data object PublishClicked : SetlistEditorEvent
}
