package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.WeeklySetlist
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import com.xperiencemusic.shared.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Every kept setlist with its songs, in setlist order. Unknown song ids are skipped. */
class ObserveSetlistScheduleUseCase(
    private val setlists: SetlistRepository,
    private val songs: SongRepository,
    private val dates: DateProvider,
) {
    operator fun invoke(): Flow<SetlistSchedule> =
        combine(setlists.observeSetlists(), songs.observeSongs()) { all, library ->
            val byId = library.associateBy { it.id }
            SetlistSchedule.of(all.map { WeeklySetlist(it, it.songIds.mapNotNull(byId::get)) }, dates.today())
        }
}
