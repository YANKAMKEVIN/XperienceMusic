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

/**
 * The setlists around today, by service date. The current week is the next service still ahead
 * (or the last one when none is planned yet); the ones after it were sent in advance.
 */
data class SetlistSchedule(val weeks: List<WeeklySetlist>, val currentIndex: Int) {
    val current: WeeklySetlist? get() = weeks.getOrNull(currentIndex)

    /** The current week and those sent ahead: what to download before rehearsing. */
    val ahead: List<WeeklySetlist> get() = if (currentIndex < 0) emptyList() else weeks.drop(currentIndex)

    /**
     * The week whose order a song follows on stage: the current week when it is in it, otherwise
     * the next week ahead that has it, otherwise the latest past week that had it.
     */
    fun weekOf(songId: String): WeeklySetlist? {
        fun WeeklySetlist.has() = songs.any { it.id == songId }
        return current?.takeIf { it.has() }
            ?: ahead.firstOrNull { it.has() }
            ?: weeks.lastOrNull { it.has() }
    }

    companion object {
        val EMPTY = SetlistSchedule(emptyList(), -1)

        fun of(weeks: List<WeeklySetlist>, today: LocalDate): SetlistSchedule {
            val sorted = weeks.sortedBy { it.setlist.serviceDate }
            val next = sorted.indexOfFirst { it.setlist.serviceDate >= today }
            return SetlistSchedule(sorted, if (next >= 0) next else sorted.lastIndex)
        }
    }
}

/** How much of the week's audio is on the phone. */
data class OfflineAvailability(val available: Int, val total: Int) {
    val isComplete: Boolean get() = total > 0 && available >= total
}
