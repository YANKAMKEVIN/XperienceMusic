package com.xperiencemusic.shared.presentation.week

import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.usecase.Planning
import com.xperiencemusic.shared.fakes.member
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServingThisWeekTest {
    private val october = LocalDate(2026, 10, 1)
    private val friday = LocalDate(2026, 10, 16)
    private val sunday = LocalDate(2026, 10, 18)
    private val wednesday = LocalDate(2026, 10, 14)

    private fun planning(vararg signups: ServiceSignup, validated: Boolean = true, months: List<PlanningMonth>? = null) = Planning(
        months = months ?: listOf(PlanningMonth(october, null, validated)),
        signups = signups.toList(),
        members = emptyList(),
        me = member(),
    )

    @Test
    fun `le vendredi et le dimanche où je sers, dans l'ordre`() {
        val serving = deriveServingThisWeek(
            planning(ServiceSignup(sunday, "me", true), ServiceSignup(friday, "me", true)),
            wednesday,
        )
        assertEquals(listOf(friday, sunday), serving?.dates)
        assertTrue(serving!!.isConfirmed)
    }

    @Test
    fun `rien quand je ne sers pas cette semaine`() {
        val notMe = planning(
            ServiceSignup(friday, "me", false),
            ServiceSignup(sunday, "someone-else", true),
            // The next week's Sunday is not "cette semaine".
            ServiceSignup(LocalDate(2026, 10, 25), "me", true),
        )
        assertNull(deriveServingThisWeek(notMe, wednesday))
    }

    @Test
    fun `le vendredi passé ne compte plus le samedi`() {
        val serving = deriveServingThisWeek(
            planning(ServiceSignup(friday, "me", true), ServiceSignup(sunday, "me", true)),
            LocalDate(2026, 10, 17),
        )
        assertEquals(listOf(sunday), serving?.dates)
    }

    @Test
    fun `le jour même, le service du jour est encore là`() {
        val serving = deriveServingThisWeek(planning(ServiceSignup(sunday, "me", true)), sunday)
        assertEquals(listOf(sunday), serving?.dates)
    }

    @Test
    fun `à confirmer tant que le mois n'est pas validé`() {
        val serving = deriveServingThisWeek(planning(ServiceSignup(friday, "me", true), validated = false), wednesday)
        assertFalse(serving!!.isConfirmed)
    }

    @Test
    fun `à cheval sur deux mois, il faut que les deux soient validés`() {
        // Friday 30 Oct. and Sunday 1 Nov.
        val signups = arrayOf(ServiceSignup(LocalDate(2026, 10, 30), "me", true), ServiceSignup(LocalDate(2026, 11, 1), "me", true))
        val thursday = LocalDate(2026, 10, 29)
        val november = LocalDate(2026, 11, 1)
        val onlyOctober = planning(*signups, months = listOf(PlanningMonth(october, null, true), PlanningMonth(november, null, false)))
        assertFalse(deriveServingThisWeek(onlyOctober, thursday)!!.isConfirmed)
        val both = planning(*signups, months = listOf(PlanningMonth(october, null, true), PlanningMonth(november, null, true)))
        assertEquals(2, deriveServingThisWeek(both, thursday)!!.dates.size)
        assertTrue(deriveServingThisWeek(both, thursday)!!.isConfirmed)
    }

    @Test
    fun `rien sans membre connecté`() {
        assertNull(deriveServingThisWeek(planning(ServiceSignup(sunday, "me", true)).copy(me = null), wednesday))
    }
}
