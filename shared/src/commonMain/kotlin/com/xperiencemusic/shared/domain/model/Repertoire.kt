package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.LocalDate

/** The songs the band sings, as opposed to the whole catalogue. */
data class Repertoire(
    /** The next service still ahead, when its setlist is published. */
    val nextService: LocalDate?,
    /** Its songs, in setlist order. */
    val nextServiceSongs: List<Song>,
    /** The latest past service of each song that was in one, by song id. */
    val lastSungOn: Map<String, LocalDate>,
    /** Every song of the repertoire, in no particular order. */
    val songs: List<Song>,
) {
    companion object {
        val EMPTY = Repertoire(null, emptyList(), emptyMap(), emptyList())
    }
}
