package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.Reminder
import com.xperiencemusic.shared.domain.model.ServiceKind
import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.monthStart
import com.xperiencemusic.shared.domain.model.serviceKind
import com.xperiencemusic.shared.domain.model.servicesOf
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
private val ANSWERS_DUE_TIME = LocalTime(18, 0)

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
        .map { (month, dates) -> Reminder.MonthValidated(month, dates, at = context.now) }

    val rehearsals = confirmed.filter { it.serviceKind() == ServiceKind.SUNDAY }.map { sunday ->
        val songs = schedule.weeks.firstOrNull { it.setlist.serviceDate == sunday }?.songs?.map { it.title }.orEmpty()
        // Rehearsal is on Tuesday: the reminder comes on Monday evening.
        Reminder.RehearsalEve(sunday, songs, at = LocalDateTime(sunday.minus(DatePeriod(days = 6)), EVENING))
    }

    val eves = confirmed.map { date ->
        Reminder.ServiceEve(date, date.serviceKind() ?: ServiceKind.SUNDAY, at = LocalDateTime(date.minus(DatePeriod(days = 1)), EVENING))
    }

    val nudges = planning.months.filter { !it.isValidated && it.answerBy != null }.mapNotNull { month ->
        val missing = servicesOf(month.monthStart).count { date ->
            date >= today && planning.signups.none { it.memberId == me.id && it.serviceDate == date }
        }
        if (missing == 0) return@mapNotNull null
        val answerBy = month.answerBy!!
        Reminder.AnswersDue(month.monthStart, missing, answerBy, at = LocalDateTime(answerBy.minus(DatePeriod(days = 1)), ANSWERS_DUE_TIME))
    }

    return (announcements + (rehearsals + eves + nudges).filter { it.at > context.now }).sortedBy { it.at }
}
