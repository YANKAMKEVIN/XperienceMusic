package com.xperiencemusic.shared.presentation.planning

import com.xperiencemusic.shared.domain.model.BandMember
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.usecase.Planning
import com.xperiencemusic.shared.fakes.member
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanningStateTest {
    private val today = LocalDate(2026, 10, 15)
    private val october = LocalDate(2026, 10, 1)
    private val november = LocalDate(2026, 11, 1)
    private val band = listOf(
        BandMember("me", "me@x", "Grâce", Role.MEMBER, Instrument.SINGER, VoicePart.ALTO),
        BandMember("d", "d@x", "Daniel", Role.MEMBER, Instrument.PIANO, null),
    )

    private fun planning(role: Role = Role.MEMBER, months: List<PlanningMonth>, signups: List<ServiceSignup> = emptyList()) =
        Planning(months, signups, band, member(role))

    @Test
    fun `un membre voit ce mois et les mois ouverts, un admin aussi les deux suivants`() {
        val months = listOf(PlanningMonth(october, null, true))
        assertEquals(listOf(october), planningMonths(today, months, canPlan = false))
        assertEquals(listOf(october, november, LocalDate(2026, 12, 1)), planningMonths(today, months, canPlan = true))
        // A month already open further ahead stays reachable for everyone.
        val march = LocalDate(2027, 3, 1)
        assertEquals(listOf(october, march), planningMonths(today, months + PlanningMonth(march, null, false), canPlan = false))
    }

    @Test
    fun `un mois ouvert compte les services passés à part et les réponses manquantes`() {
        val months = listOf(PlanningMonth(october, LocalDate(2026, 9, 24), false))
        val signups = listOf(ServiceSignup(LocalDate(2026, 10, 16), "me", true))
        val state = derivePlanningState(planning(months = months, signups = signups), october, today)
        assertEquals(MonthStatus.OPEN, state.status)
        assertEquals("octobre 2026", state.monthLabel)
        // Oct. 2026: Fridays 2, 9, 16, 23, 30 and Sundays 4, 11, 18, 25; from the 15th on, 5 left, one answered.
        assertEquals(9, state.services.size)
        assertEquals(4, state.unansweredCount)
        assertEquals(MyAnswer.SERVES, state.services.first { it.day == 16 }.myAnswer)
        assertTrue(state.services.first { it.day == 2 }.isPast)
        assertFalse(state.services.first { it.day == 2 }.showsAnswer, "too late to answer a past service")
        assertTrue(state.services.first { it.day == 18 }.showsAnswer)

        val validated = derivePlanningState(planning(months = listOf(PlanningMonth(october, null, true))), october, today)
        assertTrue(validated.services.none { it.showsAnswer }, "nothing left to answer once validated")
    }

    @Test
    fun `valider un mois rappelle les réponses manquantes et les parties non couvertes`() {
        val months = listOf(PlanningMonth(october, null, false))
        val signups = listOf(
            ServiceSignup(LocalDate(2026, 10, 16), "me", true),
            ServiceSignup(LocalDate(2026, 10, 16), "d", true),
            ServiceSignup(LocalDate(2026, 10, 18), "d", false),
            // A past service: no longer counted.
            ServiceSignup(LocalDate(2026, 10, 2), "me", true),
        )
        val validation = derivePlanningState(planning(Role.CONTENT_ADMIN, months, signups), october, today).validation
        // From the 15th: Fridays 16, 23, 30 and Sundays 18, 25, for two members; three answers given.
        assertEquals(5 * 2 - 3, validation?.missingAnswers)
        // Answers and people are two different counts: both members still owe at least one date.
        assertEquals(2, validation?.missingMembers)
        val allMine = listOf(16, 18, 23, 25, 30).map { ServiceSignup(LocalDate(2026, 10, it), "me", true) }
        val onlyDaniel = derivePlanningState(planning(Role.CONTENT_ADMIN, months, allMine), october, today).validation
        assertEquals(5, onlyDaniel?.missingAnswers)
        assertEquals(1, onlyDaniel?.missingMembers)
        // Nobody plays bass or drums, on any of the five; the 16th has its singer and pianist.
        assertEquals(5, validation?.incompleteServices)
        assertEquals(listOf(Instrument.SINGER, Instrument.PIANO, Instrument.BASS, Instrument.DRUMS), validation?.uncoveredParts)

        assertEquals(null, derivePlanningState(planning(months = months, signups = signups), october, today).validation, "members cannot validate")
        val validated = listOf(PlanningMonth(october, null, true))
        assertEquals(null, derivePlanningState(planning(Role.CONTENT_ADMIN, validated), october, today).validation)
    }

    @Test
    fun `une échéance dépassée se dit, le jour même est signalé`() {
        // A Friday with a service, six days after the deadline.
        val friday = LocalDate(2026, 10, 16)
        val late = listOf(PlanningMonth(october, LocalDate(2026, 10, 10), false))
        val state = derivePlanningState(planning(months = late), october, friday)
        assertTrue(state.isPastDeadline)
        assertEquals("10 oct.", state.answerByLabel)
        assertTrue(state.services.single { it.day == 16 }.isToday)
        assertEquals(1, state.services.count { it.isToday })
        assertTrue(deriveAvailabilityState(planning(months = late), october, friday).isPastDeadline)

        val onTime = listOf(PlanningMonth(october, LocalDate(2026, 10, 20), false))
        assertFalse(derivePlanningState(planning(months = onTime), october, friday).isPastDeadline)
    }

    @Test
    fun `un mois validé mais incomplet le dit dans le bandeau`() {
        val validated = listOf(PlanningMonth(october, LocalDate(2026, 10, 10), true))
        val state = derivePlanningState(planning(months = validated), october, today)
        assertFalse(state.isPastDeadline, "a validated month has no deadline left")
        // Nobody answered: the five services still to come all lack a part.
        assertEquals(5, state.incompleteAfterValidation)
    }

    @Test
    fun `un mois pas ouvert ne montre aucun service`() {
        val state = derivePlanningState(planning(Role.CONTENT_ADMIN, months = emptyList()), november, today)
        assertEquals(MonthStatus.NOT_OPEN, state.status)
        assertTrue(state.services.isEmpty())
        assertTrue(state.canPlan)
        assertTrue(state.canGoBack)
    }

    @Test
    fun `un mois validé se lit sans pouvoir répondre, sauf pour un admin`() {
        val months = listOf(PlanningMonth(november, null, true))
        assertFalse(deriveAvailabilityState(planning(months = months), november, today).canAnswer)
        assertTrue(deriveAvailabilityState(planning(Role.CONTENT_ADMIN, months = months), november, today).canAnswer)
    }

    @Test
    fun `le détail d'un service liste l'équipe et donne aux admins tout le groupe`() {
        val friday = LocalDate(2026, 11, 6)
        val months = listOf(PlanningMonth(november, null, false))
        val signups = listOf(ServiceSignup(friday, "d", true))
        val empty = SetlistSchedule(emptyList(), -1)
        val asMember = deriveServiceDetail(planning(months = months, signups = signups), empty, friday, today)
        assertEquals(listOf("Daniel"), asMember.serving.map { it.name })
        assertEquals(listOf(Instrument.SINGER, Instrument.BASS, Instrument.DRUMS), asMember.missing)
        assertTrue(asMember.roster.isEmpty())

        val asAdmin = deriveServiceDetail(planning(Role.CONTENT_ADMIN, months = months, signups = signups), empty, friday, today)
        assertEquals(listOf("Grâce" to null, "Daniel" to true), asAdmin.roster.map { it.member.name to it.serves })
    }
}
