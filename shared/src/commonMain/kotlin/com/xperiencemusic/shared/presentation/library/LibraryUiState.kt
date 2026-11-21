package com.xperiencemusic.shared.presentation.library

import kotlinx.datetime.LocalDate

data class LibraryUiState(
    val query: String = "",
    val tab: LibraryTab = LibraryTab.REPERTOIRE,
    val repertoireCount: Int = 0,
    val catalogueCount: Int = 0,
    val isLoading: Boolean = true,
    /** What the list shows: the chosen tab, or the matches of [query] across the whole catalogue. */
    val sections: List<LibrarySectionUi> = emptyList(),
    val canAddSong: Boolean = false,
    val isRefreshing: Boolean = false,
    /** The server could not be reached: what is shown is the copy on the phone. */
    val isOffline: Boolean = false,
    /** The first load failed and the phone has no copy yet: nothing to show but a way to retry. */
    val hasLoadFailed: Boolean = false,
    /** The themes of the catalogue, A to Z; empty (and the filter hidden) while no song has one. */
    val themes: List<String> = emptyList(),
    /** The theme the lists and the search are narrowed to; null for every song. */
    val selectedTheme: String? = null,
) {
    val isSearching: Boolean get() = query.isNotBlank()
}

enum class LibraryTab {
    /** The songs the band sings: those of a setlist or with a recording. */
    REPERTOIRE,
    /** Every song, from A to Z. */
    CATALOGUE,
}

data class LibrarySectionUi(val header: LibrarySectionHeader, val songs: List<LibrarySongUi>)

sealed interface LibrarySectionHeader {
    data class NextService(val date: LocalDate) : LibrarySectionHeader
    data object RecentlySung : LibrarySectionHeader
    data object Alphabetical : LibrarySectionHeader
    /** "A" to "Z", or "#" for titles starting with anything else. */
    data class Letter(val letter: String) : LibrarySectionHeader
    data class Results(val count: Int) : LibrarySectionHeader
}

data class LibrarySongUi(
    val id: String,
    val title: String,
    val artist: String?,
    val hasAudio: Boolean,
    /** Set in "recently sung". */
    val lastSungOn: LocalDate? = null,
    /** The line of the lyrics a search matched, when the title and the artist did not. */
    val lyricLine: String? = null,
)

sealed interface LibraryEvent {
    data class QueryChanged(val query: String) : LibraryEvent
    data class TabSelected(val tab: LibraryTab) : LibraryEvent
    /** Null shows every theme again. */
    data class ThemeSelected(val theme: String?) : LibraryEvent
    data object RefreshPulled : LibraryEvent
}
