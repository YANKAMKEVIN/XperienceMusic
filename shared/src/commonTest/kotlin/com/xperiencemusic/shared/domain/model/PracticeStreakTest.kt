package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PracticeStreakTest {
    private val today = LocalDate(2026, 10, 12)
    private fun daysAgo(vararg offsets: Int) = offsets.map { LocalDate(2026, 10, 12 - it) }.toSet()

    @Test
    fun `jours d'affilée jusqu'à aujourd'hui`() {
        assertEquals(3, practiceStreak(daysAgo(0, 1, 2), today))
    }

    @Test
    fun `aujourd'hui reste ouvert tant qu'il n'est pas fini`() {
        assertEquals(2, practiceStreak(daysAgo(1, 2), today))
    }

    @Test
    fun `un jour de pause par semaine ne casse pas la série`() {
        assertEquals(4, practiceStreak(daysAgo(0, 1, 3, 4), today))
    }

    @Test
    fun `deux pauses dans la même semaine l'arrêtent`() {
        // The 10th is forgiven, the 8th is the second pause in a week: 12, 11 and 9 count.
        assertEquals(3, practiceStreak(daysAgo(0, 1, 3, 5, 6), today))
    }

    @Test
    fun `sans entraînement la série est à zéro`() {
        assertEquals(0, practiceStreak(emptySet(), today))
        assertEquals(0, practiceStreak(daysAgo(5), today))
    }
}
