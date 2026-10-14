package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.BandMember
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.model.monthStart
import com.xperiencemusic.shared.domain.model.serviceKind
import com.xperiencemusic.shared.domain.repository.MembersRepository
import com.xperiencemusic.shared.domain.repository.PlanningRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/** Everything the planning screens show. */
data class Planning(
    val months: List<PlanningMonth>,
    val signups: List<ServiceSignup>,
    val members: List<BandMember>,
    val me: Member?,
)

class ObservePlanningUseCase(
    private val planning: PlanningRepository,
    private val members: MembersRepository,
    private val observeCurrentMember: ObserveCurrentMemberUseCase,
) {
    operator fun invoke(): Flow<Planning> = combine(
        planning.observeMonths(),
        planning.observeSignups(),
        members.observeMembers(),
        observeCurrentMember(),
        ::Planning,
    )
}

/**
 * The signed-in member says whether they serve on [serviceDate]. Only while its month is open and
 * not validated yet; an admin may still answer afterwards, as for anyone (see [AdjustServiceUseCase]).
 */
class AnswerServiceUseCase(
    private val sessions: SessionRepository,
    private val planning: PlanningRepository,
) {
    suspend operator fun invoke(serviceDate: LocalDate, serves: Boolean): DomainResult<Unit> {
        val me = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (serviceDate.serviceKind() == null) return DomainResult.Failure(DomainError.INVALID_INPUT)
        if (!me.role.can(Permission.PLAN_SERVICES)) {
            val month = planning.observeMonths().first().firstOrNull { it.monthStart == serviceDate.monthStart() }
            if (month == null || month.isValidated) return DomainResult.Failure(DomainError.FORBIDDEN)
        }
        return runRemote { planning.answer(me.id, serviceDate, serves) }
    }
}

/** An admin puts [memberId] on, or off, the team of [serviceDate], whatever the month's state. */
class AdjustServiceUseCase(
    private val sessions: SessionRepository,
    private val planning: PlanningRepository,
) {
    suspend operator fun invoke(memberId: String, serviceDate: LocalDate, serves: Boolean): DomainResult<Unit> {
        val me = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (!me.role.can(Permission.PLAN_SERVICES)) return DomainResult.Failure(DomainError.FORBIDDEN)
        if (serviceDate.serviceKind() == null) return DomainResult.Failure(DomainError.INVALID_INPUT)
        return runRemote { planning.answer(memberId, serviceDate, serves) }
    }
}

/** An admin opens a month for sign-ups, with an optional date to answer by. */
class OpenPlanningMonthUseCase(
    private val sessions: SessionRepository,
    private val planning: PlanningRepository,
) {
    suspend operator fun invoke(monthStart: LocalDate, answerBy: LocalDate?): DomainResult<Unit> {
        val me = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (!me.role.can(Permission.PLAN_SERVICES)) return DomainResult.Failure(DomainError.FORBIDDEN)
        if (monthStart.day != 1 || (answerBy != null && answerBy >= monthStart)) {
            return DomainResult.Failure(DomainError.INVALID_INPUT)
        }
        return runRemote { planning.openMonth(monthStart, answerBy) }
    }
}

/** An admin closes the sign-ups: the teams are final and members get told they serve. */
class ValidatePlanningMonthUseCase(
    private val sessions: SessionRepository,
    private val planning: PlanningRepository,
) {
    suspend operator fun invoke(monthStart: LocalDate): DomainResult<Unit> {
        val me = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (!me.role.can(Permission.PLAN_SERVICES)) return DomainResult.Failure(DomainError.FORBIDDEN)
        return runRemote { planning.validateMonth(monthStart) }
    }
}
