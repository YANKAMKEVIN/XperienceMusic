package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.repository.AudioCache
import com.xperiencemusic.shared.domain.repository.AudioUrlProvider

/** Plays the downloaded copy when there is one, so listening works without network. */
class OfflineFirstAudioUrlProvider(
    private val cache: AudioCache,
    private val remote: AudioUrlProvider,
) : AudioUrlProvider {
    override suspend fun playableUrl(audioPath: String): String =
        cache.localUrl(audioPath) ?: remote.playableUrl(audioPath)
}
