package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.BandMember
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.LevelProgress
import com.xperiencemusic.shared.domain.model.LevelRequest
import com.xperiencemusic.shared.domain.model.Objective
import com.xperiencemusic.shared.domain.model.ObjectiveCheck
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.levelProgress
import com.xperiencemusic.shared.domain.repository.GrowthRepository
import com.xperiencemusic.shared.domain.repository.MembersRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** The signed-in member's way to the next level, with their musician objectives. */
data class MyGrowth(
    val progress: LevelProgress,
    /** The objectives of the next level, for musicians; empty for singers. */
    val objectives: List<Objective>,
    val checks: List<ObjectiveCheck>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveMyGrowthUseCase(
    private val observeCurrentMember: ObserveCurrentMemberUseCase,
    private val observeSingerPath: ObserveSingerPathUseCase,
    private val growth: GrowthRepository,
    private val objectives: List<Objective>,
) {
    operator fun invoke(): Flow<MyGrowth?> = observeCurrentMember().flatMapLatest { member ->
        if (member == null) return@flatMapLatest flowOf(null)
        val isSinger = member.profile.instrument == Instrument.SINGER
        val path = if (isSinger) observeSingerPath(member.id) else flowOf(null)
        combine(path, growth.observeRequests(), growth.observeChecks()) { singerPath, requests, checks ->
            val mine = checks.filter { it.memberId == member.id }
            val progress = levelProgress(member, singerPath, objectives, mine, requests.filter { it.memberId == member.id })
            MyGrowth(
                progress = progress,
                objectives = if (isSinger) emptyList() else objectives.filter { it.instrument == member.profile.instrument && it.level == progress.next },
                checks = mine,
            )
        }
    }
}

/** Asks the super admin to move up, once everything the next level needs is done. */
class RequestLevelUseCase(
    private val sessions: SessionRepository,
    private val observeMyGrowth: ObserveMyGrowthUseCase,
    private val growth: GrowthRepository,
) {
    suspend operator fun invoke(): DomainResult<Unit> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        val progress = observeMyGrowth().first()?.progress ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        val target = progress.next
        if (target == null || !progress.canAsk) return DomainResult.Failure(DomainError.INVALID_INPUT)
        return runRemote { growth.requestLevel(member.id, target) }
    }
}

/** A musician says they reached an objective, or takes it back while it is not validated. */
class TickObjectiveUseCase(
    private val sessions: SessionRepository,
    private val growth: GrowthRepository,
    private val objectives: List<Objective>,
) {
    suspend operator fun invoke(objectiveId: String, reached: Boolean): DomainResult<Unit> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        val objective = objectives.firstOrNull { it.id == objectiveId } ?: return DomainResult.Failure(DomainError.NOT_FOUND)
        if (objective.instrument != member.profile.instrument) return DomainResult.Failure(DomainError.FORBIDDEN)
        val current = growth.observeChecks().first().firstOrNull { it.memberId == member.id && it.objectiveId == objectiveId }
        if (current?.status == ExerciseStatus.VALIDATED) return DomainResult.Failure(DomainError.FORBIDDEN)
        return runRemote {
            if (reached) growth.tickObjective(member.id, objectiveId) else growth.untickObjective(member.id, objectiveId)
        }
    }
}

/** Everything waiting for the super admin: level-up requests and ticked objectives. */
data class GrowthReview(val requests: List<LevelRequest>, val checks: List<ObjectiveCheck>)

class ObserveGrowthReviewUseCase(private val growth: GrowthRepository) {
    operator fun invoke(): Flow<GrowthReview> = combine(growth.observeRequests(), growth.observeChecks(), ::GrowthReview)
}

/** The super admin confirms a level-up or sends it back; sending back needs a comment. */
class ReviewLevelRequestUseCase(
    private val sessions: SessionRepository,
    private val growth: GrowthRepository,
) {
    suspend operator fun invoke(requestId: String, validated: Boolean, feedback: String): DomainResult<Unit> {
        val reviewer = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (!reviewer.role.can(Permission.REVIEW_TRAINING)) return DomainResult.Failure(DomainError.FORBIDDEN)
        val comment = feedback.trim().ifEmpty { null }
        if (!validated && comment == null) return DomainResult.Failure(DomainError.INVALID_INPUT)
        return runRemote { growth.reviewRequest(requestId, if (validated) ExerciseStatus.VALIDATED else ExerciseStatus.TO_REWORK, comment) }
    }
}

/** The super admin validates a musician's objective or sends it back with a comment. */
class ReviewObjectiveUseCase(
    private val sessions: SessionRepository,
    private val growth: GrowthRepository,
) {
    suspend operator fun invoke(memberId: String, objectiveId: String, validated: Boolean, feedback: String): DomainResult<Unit> {
        val reviewer = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (!reviewer.role.can(Permission.REVIEW_TRAINING)) return DomainResult.Failure(DomainError.FORBIDDEN)
        val comment = feedback.trim().ifEmpty { null }
        if (!validated && comment == null) return DomainResult.Failure(DomainError.INVALID_INPUT)
        return runRemote {
            growth.reviewObjective(memberId, objectiveId, if (validated) ExerciseStatus.VALIDATED else ExerciseStatus.TO_REWORK, comment)
        }
    }
}

/** The super admin corrects a member's level, e.g. one chosen too high or too low at onboarding. */
class ChangeMemberLevelUseCase(
    private val sessions: SessionRepository,
    private val members: MembersRepository,
) {
    suspend operator fun invoke(member: BandMember, level: Level): DomainResult<Unit> {
        when (val check = requireMemberManager(sessions)) {
            is DomainResult.Failure -> return check
            is DomainResult.Success -> Unit
        }
        if (member.displayName == null) return DomainResult.Failure(DomainError.INVALID_INPUT)
        return runRemote { members.changeLevel(member, level) }
    }
}
