package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.Reminder
import com.xperiencemusic.shared.domain.model.ServiceKind
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.WeeklySetlist
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.song
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceRemindersTest {
    private val november = LocalDate(2026, 11, 1)
    private val now = LocalDateTime(2026, 10, 28, 12, 0)
    private val sunday8 = LocalDate(2026, 11, 8)
    private val friday13 = LocalDate(2026, 11, 13)
    private val schedule = SetlistSchedule(
        listOf(WeeklySetlist(Setlist("w", LocalDate(2026, 11, 3), sunday8, listOf("a"), null, "x"), listOf(song("a", "À toi la gloire")))),
        0,
    )

    private fun planning(validated: Boolean, answerBy: LocalDate? = null, signups: List<ServiceSignup>) =
        Planning(listOf(PlanningMonth(november, answerBy, validated)), signups, emptyList(), member())

    @Test
    fun `un mois validé annonce les services une seule fois puis rappelle chaque veille`() {
        val signups = listOf(ServiceSignup(sunday8, "me", true), ServiceSignup(friday13, "me", true), ServiceSignup(LocalDate(2026, 11, 15), "me", false))
        val reminders = planServiceReminders(planning(true, signups = signups), schedule, ReminderContext(now, emptySet()))
        assertEquals(
            listOf(
                Reminder.MonthValidated(november, listOf(sunday8, friday13), now),
                Reminder.RehearsalEve(sunday8, listOf("À toi la gloire"), LocalDateTime(2026, 11, 2, 19, 0)),
                Reminder.ServiceEve(sunday8, ServiceKind.SUNDAY, LocalDateTime(2026, 11, 7, 19, 0)),
                Reminder.ServiceEve(friday13, ServiceKind.FRIDAY, LocalDateTime(2026, 11, 12, 19, 0)),
            ),
            reminders,
        )
        assertEquals(
            listOf("validated-2026-11-01", "rehearsal-2026-11-08", "eve-2026-11-08", "eve-2026-11-13"),
            reminders.map { it.id },
        )

        val again = planServiceReminders(planning(true, signups = signups), schedule, ReminderContext(now, setOf(november)))
        assertTrue(again.none { it is Reminder.MonthValidated })
    }

    @Test
    fun `rien n'est rappelé tant que le mois n'est pas validé, sauf les réponses manquantes`() {
        val signups = listOf(ServiceSignup(sunday8, "me", true))
        val reminders = planServiceReminders(planning(false, LocalDate(2026, 10, 30), signups), schedule, ReminderContext(now, emptySet()))
        assertEquals(
            listOf(Reminder.AnswersDue(november, missing = 8, answerBy = LocalDate(2026, 10, 30), at = LocalDateTime(2026, 10, 29, 18, 0))),
            reminders,
        )
    }

    @Test
    fun `les rappels passés sont abandonnés`() {
        val late = LocalDateTime(2026, 11, 7, 20, 0)
        val reminders = planServiceReminders(planning(true, signups = listOf(ServiceSignup(sunday8, "me", true))), schedule, ReminderContext(late, setOf(november)))
        assertTrue(reminders.isEmpty())
    }
}
