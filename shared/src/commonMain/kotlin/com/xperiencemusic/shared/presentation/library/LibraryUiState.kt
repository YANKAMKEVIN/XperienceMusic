package com.xperiencemusic.shared.presentation.library

data class LibraryUiState(
    val query: String = "",
    val isLoading: Boolean = true,
    val songs: List<LibrarySongUi> = emptyList(),
    val canAddSong: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
)

data class LibrarySongUi(
    val id: String,
    val title: String,
    val artist: String?,
    val keyLabel: String,
    val recordingCount: Int,
    val maxRecordings: Int,
)

sealed interface LibraryEvent {
    data class QueryChanged(val query: String) : LibraryEvent
    data object RefreshPulled : LibraryEvent
}
