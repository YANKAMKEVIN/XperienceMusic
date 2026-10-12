package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.repository.AudioCache
import kotlinx.coroutines.flow.first

/**
 * Downloads the audio of the current week and the weeks sent ahead that is not on the phone yet, one file after the other. Stops at the
 * first failure; what was downloaded stays, so trying again resumes where it stopped.
 */
class DownloadSetlistUseCase(
    private val observeSchedule: ObserveSetlistScheduleUseCase,
    private val cache: AudioCache,
) {
    suspend operator fun invoke(): DomainResult<Unit> {
        val paths = observeSchedule().first().ahead.flatMap { it.audioPaths }.distinct()
        val missing = paths.filterNot { it in cache.cachedPaths.value }
        for (path in missing) {
            val result = runRemote { cache.download(path) }
            if (result is DomainResult.Failure) return result
        }
        return DomainResult.Success(Unit)
    }
}
