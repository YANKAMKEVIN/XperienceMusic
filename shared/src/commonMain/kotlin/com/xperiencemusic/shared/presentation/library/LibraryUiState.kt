package com.xperiencemusic.shared.presentation.library

data class LibraryUiState(
    val query: String = "",
    /** The keys of the catalogue, as pills ("Ré", "Lam"), when there are at least two. */
    val keys: List<String> = emptyList(),
    /** The key the list is narrowed to; null shows every key. */
    val selectedKey: String? = null,
    val isLoading: Boolean = true,
    val songs: List<LibrarySongUi> = emptyList(),
    val canAddSong: Boolean = false,
    val isRefreshing: Boolean = false,
    /** The server could not be reached: what is shown is the copy on the phone. */
    val isOffline: Boolean = false,
    /** The first load failed and the phone has no copy yet: nothing to show but a way to retry. */
    val hasLoadFailed: Boolean = false,
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
    data class KeySelected(val key: String?) : LibraryEvent
    data object RefreshPulled : LibraryEvent
}
