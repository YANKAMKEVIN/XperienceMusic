package com.xperiencemusic.shared.presentation.week

import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.WeeklySetlist
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeekLastPublishedTest {
    private val firstSunday = LocalDate(2026, 11, 15)
    private val secondSunday = LocalDate(2026, 11, 22)

    private fun week(id: String, service: LocalDate) =
        WeeklySetlist(Setlist(id, LocalDate(service.year, service.month, service.day - 5), service, emptyList(), null, "x"), emptyList())

    private fun state(today: LocalDate, selected: LocalDate? = null) =
        deriveWeekState(SetlistSchedule.of(listOf(week("a", firstSunday), week("b", secondSunday)), today), selected, null)

    @Test
    fun `sans setlist pour cette semaine, la dernière est annoncée comme déjà chantée`() {
        val state = state(today = LocalDate(2026, 11, 24))
        assertEquals(secondSunday, state.setlist?.serviceDate)
        assertTrue(state.setlist!!.isLastPublished)
        assertEquals(listOf(false, true), state.weeks.map { it.isLastPublished })
        // It stays the current week: the leaders can still drop the rehearsal or edit it.
        assertEquals(WeekTiming.CURRENT, state.setlist.timing)
    }

    @Test
    fun `une setlist dont le service est à venir reste celle de la semaine`() {
        val state = state(today = LocalDate(2026, 11, 22))
        assertEquals(secondSunday, state.setlist?.serviceDate)
        assertFalse(state.setlist!!.isLastPublished)
        assertTrue(state.weeks.none { it.isLastPublished })
    }

    @Test
    fun `une semaine passée choisie à la main garde son propre libellé`() {
        val state = state(today = LocalDate(2026, 11, 24), selected = firstSunday)
        assertEquals(WeekTiming.PAST, state.setlist?.timing)
        assertFalse(state.setlist!!.isLastPublished)
    }
}
