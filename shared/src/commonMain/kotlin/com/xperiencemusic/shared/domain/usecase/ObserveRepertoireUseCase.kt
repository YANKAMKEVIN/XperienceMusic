package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.Repertoire
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The songs the band actually sings, out of a catalogue that is mostly imported: those of a kept
 * setlist, and those with a recording.
 */
class ObserveRepertoireUseCase(
    private val observeSchedule: ObserveSetlistScheduleUseCase,
    private val songs: SongRepository,
    private val dates: DateProvider,
) {
    operator fun invoke(): Flow<Repertoire> = combine(observeSchedule(), songs.observeSongs()) { schedule, library ->
        val today = dates.today()
        val next = schedule.current?.takeIf { it.setlist.serviceDate >= today }
        val lastSungOn = buildMap {
            // Weeks are sorted by date: the latest past service of a song wins.
            schedule.weeks.filter { it.setlist.serviceDate < today }.forEach { week ->
                week.songs.forEach { put(it.id, week.setlist.serviceDate) }
            }
        }
        val known = schedule.weeks.flatMap { it.songs } + library.filter { it.recordings.isNotEmpty() }
        Repertoire(
            nextService = next?.setlist?.serviceDate,
            nextServiceSongs = next?.songs.orEmpty(),
            lastSungOn = lastSungOn,
            songs = known.distinctBy { it.id },
        )
    }
}
