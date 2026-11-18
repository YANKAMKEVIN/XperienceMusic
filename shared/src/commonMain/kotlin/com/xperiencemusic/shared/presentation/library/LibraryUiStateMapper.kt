package com.xperiencemusic.shared.presentation.library

import com.xperiencemusic.shared.domain.model.Repertoire
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.usecase.SongSearchIndex

/** How many songs "recently sung" lists: a glance, the rest is in the alphabetical list. */
private const val RECENT_SONGS = 8

fun Song.toLibraryItem(): LibrarySongUi =
    LibrarySongUi(id = id, title = title, artist = artist, hasAudio = recordings.isNotEmpty())

/**
 * The list of the library. A search covers the whole catalogue whatever the tab; [chosenTab] is
 * null until the user picks one, which opens the repertoire when the band has one.
 */
fun libraryContent(index: SongSearchIndex, repertoire: Repertoire, query: String, chosenTab: LibraryTab?): LibraryUiState {
    val repertoireIds = repertoire.songs.mapTo(HashSet()) { it.id }
    // The index is sorted by title, and drops songs deleted since the setlist was published.
    val repertoireSongs = index.songs.filter { it.id in repertoireIds }
    val tab = chosenTab ?: if (repertoireSongs.isEmpty()) LibraryTab.CATALOGUE else LibraryTab.REPERTOIRE
    val sections = when {
        query.isNotBlank() -> index.search(query, inLyrics = true)
            .map { it.song.toLibraryItem().copy(lyricLine = it.lyricLine) }
            .let { if (it.isEmpty()) emptyList() else listOf(LibrarySectionUi(LibrarySectionHeader.Results(it.size), it)) }
        tab == LibraryTab.REPERTOIRE -> repertoireSections(repertoire, repertoireSongs)
        else -> alphabeticalSections(index.songs)
    }
    return LibraryUiState(
        query = query,
        tab = tab,
        repertoireCount = repertoireSongs.size,
        catalogueCount = index.songs.size,
        isLoading = false,
        sections = sections,
    )
}

private fun repertoireSections(repertoire: Repertoire, sortedSongs: List<Song>): List<LibrarySectionUi> = buildList {
    val nextIds = repertoire.nextServiceSongs.mapTo(HashSet()) { it.id }
    repertoire.nextService?.takeIf { nextIds.isNotEmpty() }?.let { date ->
        add(LibrarySectionUi(LibrarySectionHeader.NextService(date), repertoire.nextServiceSongs.map { it.toLibraryItem() }))
    }
    val recent = sortedSongs
        .mapNotNull { song -> repertoire.lastSungOn[song.id]?.takeIf { song.id !in nextIds }?.let { song to it } }
        .sortedByDescending { it.second }
        .take(RECENT_SONGS)
        .map { (song, date) -> song.toLibraryItem().copy(lastSungOn = date) }
    if (recent.isNotEmpty()) add(LibrarySectionUi(LibrarySectionHeader.RecentlySung, recent))
    // The two sections above may already show the whole repertoire.
    val listed = nextIds + recent.map { it.id }
    if (sortedSongs.any { it.id !in listed }) {
        add(LibrarySectionUi(LibrarySectionHeader.Alphabetical, sortedSongs.map { it.toLibraryItem() }))
    }
}

/** [sortedSongs] under their first letter; accents fold ("À toi" is under A), anything else goes under "#". */
private fun alphabeticalSections(sortedSongs: List<Song>): List<LibrarySectionUi> =
    sortedSongs.groupBy { it.indexLetter() }
        .map { (letter, songs) -> LibrarySectionUi(LibrarySectionHeader.Letter(letter), songs.map { it.toLibraryItem() }) }
        // Digits and punctuation sort before the letters, but an index ends with "#".
        .sortedBy { (it.header as LibrarySectionHeader.Letter).letter == OTHER_LETTER }

private const val OTHER_LETTER = "#"

private fun Song.indexLetter(): String {
    val first = SongSearchIndex.firstLetter(title)
    return if (first in 'A'..'Z') first.toString() else OTHER_LETTER
}
