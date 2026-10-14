package com.xperiencemusic.shared.presentation.reminders

import com.xperiencemusic.shared.domain.model.Reminder
import com.xperiencemusic.shared.domain.model.ServiceKind
import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.monthStart
import com.xperiencemusic.shared.domain.model.serviceKind
import com.xperiencemusic.shared.domain.model.servicesOf
import com.xperiencemusic.shared.domain.usecase.Planning
import com.xperiencemusic.shared.presentation.common.Formatting
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus

/** What the reminders need besides the planning. */
data class ReminderContext(
    val now: LocalDateTime,
    /** Months whose validation the member was already told about. */
    val announcedMonths: Set<LocalDate>,
)

private val EVENING = LocalTime(19, 0)

/**
 * The member's reminders:
 * - once a month is validated, which services they serve (shown at once, a single time);
 * - the eve of Tuesday's rehearsal, for a Sunday they serve: work on the songs before coming;
 * - the eve of each service they serve;
 * - the day before the answers are due, while some of their answers are missing.
 * Anything already past is dropped.
 */
fun planServiceReminders(planning: Planning, schedule: SetlistSchedule, context: ReminderContext): List<Reminder> {
    val me = planning.me ?: return emptyList()
    val today = context.now.date
    val myServices = planning.signups
        .filter { it.memberId == me.id && it.serves && it.serviceDate >= today }
        .map { it.serviceDate }
        .sorted()
    val validated = planning.months.filter { it.isValidated }.map { it.monthStart }.toSet()
    val confirmed = myServices.filter { it.monthStart() in validated }

    val announcements = confirmed.groupBy { it.monthStart() }
        .filterKeys { it !in context.announcedMonths }
        .map { (month, dates) ->
            Reminder(
                id = "validated-$month",
                at = context.now,
                title = "Planning de ${Formatting.monthAndYear(month).substringBefore(' ')} validé",
                body = "Tu sers : ${dates.joinToString(", ") { Formatting.shortDayAndDate(it) }}",
            )
        }

    val rehearsals = confirmed.filter { it.serviceKind() == ServiceKind.SUNDAY }.map { sunday ->
        val songs = schedule.weeks.firstOrNull { it.setlist.serviceDate == sunday }?.songs?.map { it.title }.orEmpty()
        Reminder(
            id = "rehearsal-$sunday",
            // Rehearsal is on Tuesday: the reminder comes on Monday evening.
            at = LocalDateTime(sunday.minus(DatePeriod(days = 6)), EVENING),
            title = "Répétition demain",
            body = if (songs.isEmpty()) {
                "Travaille tes chants avant de venir : la répétition sert à mettre en commun."
            } else {
                "Travaille ${songs.joinToString(", ")} avant de venir : la répétition sert à mettre en commun."
            },
        )
    }

    val eves = confirmed.map { date ->
        val isFriday = date.serviceKind() == ServiceKind.FRIDAY
        Reminder(
            id = "eve-$date",
            at = LocalDateTime(date.minus(DatePeriod(days = 1)), EVENING),
            title = "Demain tu es de service",
            body = if (isFriday) "Vendredi : louange libre, pas de setlist." else "Dimanche : relis la setlist une dernière fois.",
        )
    }

    val nudges = planning.months.filter { !it.isValidated && it.answerBy != null }.mapNotNull { month ->
        val missing = servicesOf(month.monthStart).count { date ->
            date >= today && planning.signups.none { it.memberId == me.id && it.serviceDate == date }
        }
        if (missing == 0) return@mapNotNull null
        val answerBy = month.answerBy!!
        Reminder(
            id = "answers-${month.monthStart}",
            at = LocalDateTime(answerBy.minus(DatePeriod(days = 1)), LocalTime(18, 0)),
            title = "Tes dispos de ${Formatting.monthAndYear(month.monthStart).substringBefore(' ')}",
            body = "$missing services sans réponse. Réponds avant le ${Formatting.dayAndMonth(answerBy)} dans Planning.",
        )
    }

    return (announcements + (rehearsals + eves + nudges).filter { it.at > context.now }).sortedBy { it.at }
}
