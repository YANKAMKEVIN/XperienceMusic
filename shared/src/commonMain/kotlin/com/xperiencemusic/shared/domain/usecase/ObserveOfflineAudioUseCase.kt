package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.repository.AudioCache
import kotlinx.coroutines.flow.StateFlow

/** Audio paths downloaded on the phone. */
class ObserveOfflineAudioUseCase(private val cache: AudioCache) {
    operator fun invoke(): StateFlow<Set<String>> = cache.cachedPaths
}
