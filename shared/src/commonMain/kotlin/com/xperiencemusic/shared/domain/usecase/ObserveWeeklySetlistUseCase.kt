package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.WeeklySetlist
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import com.xperiencemusic.shared.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** The latest setlist with its songs, in setlist order. Unknown song ids are skipped. */
class ObserveWeeklySetlistUseCase(
    private val setlists: SetlistRepository,
    private val songs: SongRepository,
) {
    operator fun invoke(): Flow<WeeklySetlist?> =
        combine(setlists.observeLatest(), songs.observeSongs()) { setlist, all ->
            setlist?.let {
                val byId = all.associateBy { song -> song.id }
                WeeklySetlist(it, it.songIds.mapNotNull(byId::get))
            }
        }
}
