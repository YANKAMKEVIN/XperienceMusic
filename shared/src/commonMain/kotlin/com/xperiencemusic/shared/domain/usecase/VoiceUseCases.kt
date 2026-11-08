package com.xperiencemusic.shared.domain.usecase

import kotlinx.coroutines.flow.first
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.repository.AudioStorage
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.ExerciseRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Sends the signed-in singer's recording of [songId] for review. The file goes to their own folder
 * first; the previous recording of that song, if any, is deleted once replaced. Sending counts as
 * a day of practice.
 */
class SendVoiceUseCase(
    private val sessions: SessionRepository,
    private val storage: AudioStorage,
    private val submissions: VoiceSubmissionRepository,
    private val exercises: ExerciseRepository,
    private val dates: DateProvider,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(songId: String, wav: ByteArray, durationSeconds: Int): DomainResult<Unit> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (wav.isEmpty() || durationSeconds < MIN_SECONDS) return DomainResult.Failure(DomainError.INVALID_INPUT)
        val path = "submissions/${member.id}/$songId-${Uuid.random()}.wav"
        when (val uploaded = runRemote { storage.upload(path, WAV, wav) }) {
            is DomainResult.Failure -> return uploaded
            is DomainResult.Success -> Unit
        }
        return when (val saved = runRemote { submissions.submit(member.id, songId, path, durationSeconds) }) {
            is DomainResult.Failure -> {
                runRemote { storage.delete(path) }
                saved
            }
            is DomainResult.Success -> {
                saved.value?.let { old -> runRemote { storage.delete(old) } }
                runRemote { exercises.recordPractice(member.id, dates.today()) }
                DomainResult.Success(Unit)
            }
        }
    }

    companion object {
        const val WAV = "audio/wav"
        /** Shorter than this is a mis-tap, not a part. */
        const val MIN_SECONDS = 3
    }
}

/** The super admin's answer to a submission. */
class ReviewVoiceUseCase(
    private val sessions: SessionRepository,
    private val submissions: VoiceSubmissionRepository,
) {
    suspend operator fun invoke(submissionId: String, validated: Boolean, feedback: String): DomainResult<Unit> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (!member.role.can(Permission.REVIEW_TRAINING)) return DomainResult.Failure(DomainError.FORBIDDEN)
        // Nobody signs off their own singing: another Admin++ listens to it.
        if (submissions.observeAll().first().any { it.id == submissionId && it.memberId == member.id }) {
            return DomainResult.Failure(DomainError.FORBIDDEN)
        }
        val comment = feedback.trim().ifEmpty { null }
        // Sending work back without saying why helps no one.
        if (!validated && comment == null) return DomainResult.Failure(DomainError.INVALID_INPUT)
        return runRemote {
            submissions.review(submissionId, if (validated) ExerciseStatus.VALIDATED else ExerciseStatus.TO_REWORK, comment)
        }
    }

    /** Takes an answer back, right after it was given: the part waits for a review again. */
    suspend fun undo(submissionId: String): DomainResult<Unit> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (!member.role.can(Permission.REVIEW_TRAINING)) return DomainResult.Failure(DomainError.FORBIDDEN)
        return runRemote { submissions.review(submissionId, ExerciseStatus.SUBMITTED, null) }
    }
}
