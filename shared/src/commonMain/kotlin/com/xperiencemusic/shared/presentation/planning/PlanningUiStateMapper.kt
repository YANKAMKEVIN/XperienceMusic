package com.xperiencemusic.shared.presentation.planning

import com.xperiencemusic.shared.domain.model.BandMember
import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.model.monthStart
import com.xperiencemusic.shared.domain.model.planTeams
import com.xperiencemusic.shared.domain.usecase.Planning
import com.xperiencemusic.shared.presentation.common.Formatting
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private const val ADMIN_MONTHS_AHEAD = 2

/**
 * The months the planning shows: this one, and every later month an admin opened — admins also get
 * the next two, so they can open a month early enough to leave time for the answers.
 */
fun planningMonths(today: LocalDate, months: List<PlanningMonth>, canPlan: Boolean): List<LocalDate> {
    val thisMonth = today.monthStart()
    val opened = months.map { it.monthStart }.filter { it >= thisMonth }
    val upcoming = if (canPlan) (1..ADMIN_MONTHS_AHEAD).map { thisMonth.plus(DatePeriod(months = it)) } else emptyList()
    return (listOf(thisMonth) + opened + upcoming).distinct().sorted()
}

fun derivePlanningState(planning: Planning, monthStart: LocalDate, today: LocalDate): PlanningUiState {
    val me = planning.me
    val canPlan = me?.role?.can(Permission.PLAN_SERVICES) == true
    val month = planning.months.firstOrNull { it.monthStart == monthStart }
    val shown = planningMonths(today, planning.months, canPlan)
    val status = when {
        month == null -> MonthStatus.NOT_OPEN
        month.isValidated -> MonthStatus.VALIDATED
        else -> MonthStatus.OPEN
    }
    val services = if (month == null) {
        emptyList()
    } else {
        serviceCards(monthStart, planning.members, planning.signups, me?.id, today).map { card ->
            card.copy(showsAnswer = card.myAnswer != MyAnswer.UNANSWERED || (status == MonthStatus.OPEN && !card.isPast))
        }
    }
    return PlanningUiState(
        isLoading = false,
        monthStart = monthStart,
        monthLabel = Formatting.monthAndYear(monthStart),
        canGoBack = shown.any { it < monthStart },
        canGoForward = shown.any { it > monthStart },
        status = status,
        answerByLabel = month?.answerBy?.takeIf { status == MonthStatus.OPEN }?.let(Formatting::dayAndMonth),
        isPastDeadline = status == MonthStatus.OPEN && month?.answerBy?.let { it < today } == true,
        incompleteAfterValidation = if (status == MonthStatus.VALIDATED) services.count { !it.isPast && it.missing.isNotEmpty() } else 0,
        unansweredCount = if (status == MonthStatus.OPEN) services.count { !it.isPast && it.myAnswer == MyAnswer.UNANSWERED } else 0,
        services = services,
        canPlan = canPlan,
        validation = if (canPlan && status == MonthStatus.OPEN) monthValidation(services, planning.members, planning.signups) else null,
    )
}

/** Past services are left out: nobody can answer for them any more. */
fun monthValidation(services: List<ServiceCardUi>, members: List<BandMember>, signups: List<ServiceSignup>): MonthValidationUi {
    val coming = services.filterNot { it.isPast }
    val answered = signups.map { it.serviceDate to it.memberId }.toSet()
    return MonthValidationUi(
        missingAnswers = coming.sumOf { service -> members.count { (service.date to it.id) !in answered } },
        missingMembers = members.count { member -> coming.any { (it.date to member.id) !in answered } },
        incompleteServices = coming.count { it.missing.isNotEmpty() },
        uncoveredParts = coming.flatMap { it.missing }.distinct().sortedBy { it.ordinal },
    )
}

fun serviceCards(
    monthStart: LocalDate,
    members: List<BandMember>,
    signups: List<ServiceSignup>,
    myId: String?,
    today: LocalDate,
): List<ServiceCardUi> = planTeams(monthStart, members, signups).map { team ->
    val mine = signups.firstOrNull { it.serviceDate == team.date && it.memberId == myId }
    ServiceCardUi(
        date = team.date,
        kind = team.kind,
        weekday = Formatting.shortWeekday(team.date),
        day = team.date.day,
        serving = team.serving.map { ServingUi(it.id, it.displayName ?: it.email, it.instrument, it.voicePart, it.id == myId) },
        missing = team.missing,
        myAnswer = when (mine?.serves) {
            true -> MyAnswer.SERVES
            false -> MyAnswer.NOT_AVAILABLE
            null -> MyAnswer.UNANSWERED
        },
        isPast = team.date < today,
        isToday = team.date == today,
    )
}
