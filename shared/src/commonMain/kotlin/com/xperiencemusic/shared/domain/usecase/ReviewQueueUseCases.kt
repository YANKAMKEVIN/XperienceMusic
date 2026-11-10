package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.LevelRequest
import com.xperiencemusic.shared.domain.model.ObjectiveCheck
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import com.xperiencemusic.shared.domain.repository.GrowthRepository
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** The signed-in member's submissions, all of them or only the one of [songId]; none when signed out. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveMySubmissionsUseCase(
    private val observeCurrentMember: ObserveCurrentMemberUseCase,
    private val submissions: VoiceSubmissionRepository,
) {
    operator fun invoke(songId: String? = null): Flow<List<VoiceSubmission>> = observeCurrentMember().flatMapLatest { member ->
        if (member == null) {
            flowOf(emptyList())
        } else {
            submissions.observeMine(member.id).map { mine -> if (songId == null) mine else mine.filter { it.songId == songId } }
        }
    }
}

/**
 * Every singer's submissions, waiting or answered, for a member who reviews them
 * ([Permission.REVIEW_TRAINING]); nothing for everyone else.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveBandSubmissionsUseCase(
    private val observeCurrentMember: ObserveCurrentMemberUseCase,
    private val submissions: VoiceSubmissionRepository,
) {
    operator fun invoke(): Flow<List<VoiceSubmission>> = observeCurrentMember().flatMapLatest { member ->
        if (member?.role?.can(Permission.REVIEW_TRAINING) == true) submissions.observeAll() else flowOf(emptyList())
    }
}

/** What waits for the reviewer: sung parts to listen to, level-ups and objectives to validate. */
data class ReviewQueue(
    val voices: List<VoiceSubmission> = emptyList(),
    val levels: List<LevelRequest> = emptyList(),
    val objectives: List<ObjectiveCheck> = emptyList(),
) {
    /** The parts to listen to ("À écouter"). */
    val toReview: Int get() = voices.size

    /** The level-ups and objectives to validate ("À valider"). */
    val toValidate: Int get() = levels.size + objectives.size
}

/**
 * The reviewer's queue: only what is still [ExerciseStatus.SUBMITTED]. Empty for a member without
 * [Permission.REVIEW_TRAINING], whose screens then fetch nothing of the others' work.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveReviewQueueUseCase(
    private val observeCurrentMember: ObserveCurrentMemberUseCase,
    private val submissions: VoiceSubmissionRepository,
    private val growth: GrowthRepository,
) {
    operator fun invoke(): Flow<ReviewQueue> = observeCurrentMember().flatMapLatest { member ->
        if (member?.role?.can(Permission.REVIEW_TRAINING) != true) {
            flowOf(ReviewQueue())
        } else {
            combine(submissions.observeAll(), growth.observeRequests(), growth.observeChecks()) { voices, levels, objectives ->
                ReviewQueue(
                    voices = voices.filter { it.status == ExerciseStatus.SUBMITTED },
                    levels = levels.filter { it.status == ExerciseStatus.SUBMITTED },
                    objectives = objectives.filter { it.status == ExerciseStatus.SUBMITTED },
                )
            }
        }
    }
}
