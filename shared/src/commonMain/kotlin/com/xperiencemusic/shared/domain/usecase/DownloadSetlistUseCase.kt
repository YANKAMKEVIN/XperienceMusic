package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.repository.AudioCache
import kotlinx.coroutines.flow.first

/**
 * Downloads the week's audio that is not on the phone yet, one file after the other. Stops at the
 * first failure; what was downloaded stays, so trying again resumes where it stopped.
 */
class DownloadSetlistUseCase(
    private val observeWeeklySetlist: ObserveWeeklySetlistUseCase,
    private val cache: AudioCache,
) {
    suspend operator fun invoke(): DomainResult<Unit> {
        val weekly = observeWeeklySetlist().first() ?: return DomainResult.Success(Unit)
        val missing = weekly.audioPaths.filterNot { it in cache.cachedPaths.value }
        for (path in missing) {
            val result = runRemote { cache.download(path) }
            if (result is DomainResult.Failure) return result
        }
        return DomainResult.Success(Unit)
    }
}
