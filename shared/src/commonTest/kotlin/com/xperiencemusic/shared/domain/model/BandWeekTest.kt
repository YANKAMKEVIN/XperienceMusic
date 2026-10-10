package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BandWeekTest {
    @Test
    fun `la semaine commence au prochain mardi et finit le dimanche`() {
        // 2026-10-10 is a Saturday.
        assertEquals(BandWeek(LocalDate(2026, 10, 13), LocalDate(2026, 10, 18)), BandWeek.startingFrom(LocalDate(2026, 10, 10)))
    }

    @Test
    fun `un mardi compte pour la semaine en cours`() {
        assertEquals(LocalDate(2026, 10, 13), BandWeek.startingFrom(LocalDate(2026, 10, 13)).rehearsalDate)
    }
}
