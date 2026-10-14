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
    fun `un membre voit ce mois et les mois ouverts, un admin aussi le suivant`() {
        val months = listOf(PlanningMonth(october, null, true))
        assertEquals(listOf(october), planningMonths(today, months, canPlan = false))
        assertEquals(listOf(october, november), planningMonths(today, months, canPlan = true))
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
