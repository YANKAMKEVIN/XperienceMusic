package com.xperiencemusic.shared.presentation.planning

import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.WeeklySetlist
import com.xperiencemusic.shared.domain.usecase.Planning
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.presentation.reminders.ReminderContext
import com.xperiencemusic.shared.presentation.reminders.planServiceReminders
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
            listOf("validated-2026-11-01", "rehearsal-2026-11-08", "eve-2026-11-08", "eve-2026-11-13"),
            reminders.map { it.id },
        )
        assertEquals("Tu sers : dim. 8 nov., ven. 13 nov.", reminders.first().body)
        assertEquals(LocalDateTime(2026, 11, 2, 19, 0), reminders[1].at, "the Monday before Tuesday's rehearsal")
        assertTrue("À toi la gloire" in reminders[1].body)

        val again = planServiceReminders(planning(true, signups = signups), schedule, ReminderContext(now, setOf(november)))
        assertTrue(again.none { it.id.startsWith("validated") })
    }

    @Test
    fun `rien n'est rappelé tant que le mois n'est pas validé, sauf les réponses manquantes`() {
        val signups = listOf(ServiceSignup(sunday8, "me", true))
        val reminders = planServiceReminders(planning(false, LocalDate(2026, 10, 30), signups), schedule, ReminderContext(now, emptySet()))
        assertEquals(listOf("answers-2026-11-01"), reminders.map { it.id })
        assertEquals(LocalDateTime(2026, 10, 29, 18, 0), reminders.single().at)
        assertTrue(reminders.single().body.startsWith("8 services sans réponse"))
    }

    @Test
    fun `les rappels passés sont abandonnés`() {
        val late = LocalDateTime(2026, 11, 7, 20, 0)
        val reminders = planServiceReminders(planning(true, signups = listOf(ServiceSignup(sunday8, "me", true))), schedule, ReminderContext(late, setOf(november)))
        assertTrue(reminders.isEmpty())
    }
}
