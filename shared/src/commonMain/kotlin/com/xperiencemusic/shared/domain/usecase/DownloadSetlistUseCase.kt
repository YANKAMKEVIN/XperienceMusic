package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.repository.AudioCache
import com.xperiencemusic.shared.domain.repository.SongRepository
import kotlinx.coroutines.flow.first

/**
 * Downloads, for the current week and the weeks sent ahead, the charts (the list of songs comes
 * without them, and the stage needs them offline) and then the audio that is not on the phone yet,
 * one file after the other. Stops at the first failure; what was downloaded stays, so trying again
 * resumes where it stopped.
 */
class DownloadSetlistUseCase(
    private val observeSchedule: ObserveSetlistScheduleUseCase,
    private val cache: AudioCache,
    private val songs: SongRepository,
) {
    suspend operator fun invoke(): DomainResult<Unit> {
        val ahead = observeSchedule().first().ahead
        val songIds = ahead.flatMap { week -> week.songs.map { it.id } }.distinct()
        if (songIds.isNotEmpty()) {
            val charts = runRemote { songs.loadCharts(songIds) }
            if (charts is DomainResult.Failure) return charts
        }
        val paths = ahead.flatMap { it.audioPaths }.distinct()
        val missing = paths.filterNot { it in cache.cachedPaths.value }
        for (path in missing) {
            val result = runRemote { cache.download(path) }
            if (result is DomainResult.Failure) return result
        }
        return DomainResult.Success(Unit)
    }
}
