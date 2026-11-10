package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.Avatar
import com.xperiencemusic.shared.domain.model.AvatarPreset
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.repository.AvatarStorage
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The member's own avatar. A new photo is uploaded first and the profile switched to it after, so
 * a failed upload leaves the old avatar in place; the photo it replaces is deleted last.
 */
class ChangeAvatarUseCase(
    private val sessions: SessionRepository,
    private val storage: AvatarStorage,
) {
    suspend fun choosePreset(preset: AvatarPreset): DomainResult<Member> = save(Avatar.Preset(preset))

    @OptIn(ExperimentalUuidApi::class)
    suspend fun usePhoto(jpeg: ByteArray): DomainResult<Member> {
        if (jpeg.isEmpty()) return DomainResult.Failure(DomainError.INVALID_INPUT)
        if (jpeg.size > MAX_PHOTO_BYTES) return DomainResult.Failure(DomainError.FILE_TOO_LARGE)
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        // A new name each time: phones that cached the old photo never show it under the new path.
        val path = "${member.id}/${Uuid.random()}.jpg"
        when (val uploaded = runRemote { storage.upload(path, jpeg) }) {
            is DomainResult.Failure -> return uploaded
            is DomainResult.Success -> Unit
        }
        val result = save(Avatar.Photo(path))
        if (result is DomainResult.Failure) runRemote { storage.delete(path) }
        return result
    }

    suspend fun remove(): DomainResult<Member> = save(null)

    private suspend fun save(avatar: Avatar?): DomainResult<Member> {
        val previous = (sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)).avatar
        val result = runRemote { sessions.saveAvatar(avatar) }
        if (result is DomainResult.Success && previous is Avatar.Photo && previous != avatar) {
            // Best effort: an orphan photo only costs a few kilobytes.
            runRemote { storage.delete(previous.path) }
        }
        return result
    }

    companion object {
        /** The app sends 512 px JPEGs, far below this; the bucket refuses more than 1 MB anyway. */
        const val MAX_PHOTO_BYTES = 1_000_000
    }
}
