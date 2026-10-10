package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.LocalDate

/** The songs of the week: rehearsed on Tuesday, performed on Sunday. */
data class Setlist(
    val id: String,
    val rehearsalDate: LocalDate,
    val serviceDate: LocalDate,
    val songIds: List<String>,
    val message: String?,
    val publishedBy: String,
)

/** A setlist with its songs resolved, ready to be shown. */
data class WeeklySetlist(
    val setlist: Setlist,
    val songs: List<Song>,
) {
    /** Everything to download to rehearse offline: every version and voice track of the week. */
    val audioPaths: List<String>
        get() = songs.flatMap { song -> song.recordings.map { it.audioPath } + song.voiceTracks.map { it.audioPath } }.distinct()
}

/** How much of the week's audio is on the phone. */
data class OfflineAvailability(val available: Int, val total: Int) {
    val isComplete: Boolean get() = total > 0 && available >= total
}
