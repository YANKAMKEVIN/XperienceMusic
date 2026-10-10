package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.OfflineAvailability
import com.xperiencemusic.shared.domain.repository.AudioCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveSetlistOfflineUseCase(
    private val observeWeeklySetlist: ObserveWeeklySetlistUseCase,
    private val cache: AudioCache,
) {
    operator fun invoke(): Flow<OfflineAvailability> =
        combine(observeWeeklySetlist(), cache.cachedPaths) { weekly, cached ->
            val paths = weekly?.audioPaths.orEmpty()
            OfflineAvailability(available = paths.count { it in cached }, total = paths.size)
        }
}
