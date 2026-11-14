package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.repository.AvatarStorage
import kotlin.coroutines.cancellation.CancellationException

/**
 * A member's photo as JPEG bytes, or null when it cannot be fetched (offline, deleted): the circle
 * then shows their initials. The storage keeps fetched photos in memory, so lists of members do
 * not download the same photo again.
 */
class LoadAvatarPhotoUseCase(private val storage: AvatarStorage) {
    suspend operator fun invoke(path: String): ByteArray? =
        try {
            storage.download(path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
