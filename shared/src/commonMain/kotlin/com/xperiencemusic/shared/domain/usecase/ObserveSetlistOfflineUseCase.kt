package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.OfflineAvailability
import com.xperiencemusic.shared.domain.repository.AudioCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** How much of the current and coming weeks' audio is on the phone. */
class ObserveSetlistOfflineUseCase(
    private val observeSchedule: ObserveSetlistScheduleUseCase,
    private val cache: AudioCache,
) {
    operator fun invoke(): Flow<OfflineAvailability> =
        combine(observeSchedule(), cache.cachedPaths) { schedule, cached ->
            val paths = schedule.ahead.flatMap { it.audioPaths }.distinct()
            OfflineAvailability(available = paths.count { it in cached }, total = paths.size)
        }
}
