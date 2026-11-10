package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.learning.ActivityGrader
import com.xperiencemusic.shared.domain.learning.PianoBasics
import com.xperiencemusic.shared.domain.model.ActivityAnswer
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.GradeResult
import com.xperiencemusic.shared.domain.model.LearningActivity
import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.domain.model.LearningModule
import com.xperiencemusic.shared.domain.model.LearningProgress
import com.xperiencemusic.shared.domain.model.ObjectiveCheck
import com.xperiencemusic.shared.domain.model.ObjectiveRecording
import com.xperiencemusic.shared.domain.model.SubmissionOutcome
import com.xperiencemusic.shared.domain.model.complete
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.GrowthRepository
import com.xperiencemusic.shared.domain.repository.LearningRepository
import com.xperiencemusic.shared.domain.repository.ObjectiveRecordingRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ObserveLearningProgressUseCase(private val repository: LearningRepository) {
    operator fun invoke(memberId: String): Flow<LearningProgress> = repository.observe(memberId)
}

/** Fetches the history kept on the server and sends the attempts made offline. Never fails: they wait. */
class SyncLearningUseCase(private val repository: LearningRepository) {
    suspend operator fun invoke(memberId: String) = repository.sync(memberId)
}

/** Keeps an attempt on the phone, counted against the current version of the activities. */
class RecordLearningAttemptUseCase(
    private val repository: LearningRepository,
    private val dates: DateProvider,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        memberId: String,
        activity: LearningActivity,
        encounter: Int,
        passed: Boolean,
        hints: Int,
    ): DomainResult<Unit> = runRemote {
        repository.record(
            LearningAttempt(
                id = Uuid.random().toString(),
                memberId = memberId,
                activityId = activity.id,
                version = PianoBasics.VERSION,
                passed = passed,
                day = dates.today(),
                variant = "${activity.id}:$encounter",
                hints = hints,
            ),
        )
    }
}

/** Grades an activity and keeps the attempt; the verdict is only given once the attempt is saved. */
class GradeActivityUseCase(private val record: RecordLearningAttemptUseCase) {
    suspend operator fun invoke(
        memberId: String,
        activity: LearningActivity,
        encounter: Int,
        answer: ActivityAnswer,
        hints: Int,
    ): DomainResult<GradeResult> {
        val result = ActivityGrader.grade(activity, answer)
        return when (val saved = record(memberId, activity, encounter, result.passed, hints)) {
            is DomainResult.Success -> DomainResult.Success(result)
            is DomainResult.Failure -> saved
        }
    }
}

/** The responsable's verdicts on the member's objectives. */
class ObserveObjectiveChecksUseCase(private val growth: GrowthRepository) {
    operator fun invoke(memberId: String): Flow<List<ObjectiveCheck>> =
        growth.observeChecks().map { checks -> checks.filter { it.memberId == memberId } }
}

/** The takes sent for an objective, newest first. */
class ObserveObjectiveRecordingsUseCase(private val recordings: ObjectiveRecordingRepository) {
    operator fun invoke(memberId: String): Flow<List<ObjectiveRecording>> = recordings.observe(memberId)
        .map { rows -> rows.sortedByDescending { it.submittedAt } }
}

/**
 * Sends a take for the objective of [module] then asks for its evaluation. Only a member of the
 * module's part who finished its activities may send one.
 */
class SubmitObjectiveRecordingUseCase(
    private val sessions: SessionRepository,
    private val learning: LearningRepository,
    private val recordings: ObjectiveRecordingRepository,
    private val requestReview: TickObjectiveUseCase,
) {
    suspend operator fun invoke(module: LearningModule, wav: ByteArray, seconds: Int): DomainResult<SubmissionOutcome> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        val finished = learning.observe(member.id).first().complete(module)
        if (member.profile.instrument != module.instrument || !finished) return DomainResult.Failure(DomainError.FORBIDDEN)
        val sent = runRemote { recordings.send(member.id, module.objectiveId, wav, seconds) }
        if (sent is DomainResult.Failure) return sent
        val requested = requestReview(module.objectiveId, true)
        return DomainResult.Success(
            if (requested is DomainResult.Success) SubmissionOutcome.REVIEW_REQUESTED else SubmissionOutcome.TAKE_KEPT,
        )
    }
}
