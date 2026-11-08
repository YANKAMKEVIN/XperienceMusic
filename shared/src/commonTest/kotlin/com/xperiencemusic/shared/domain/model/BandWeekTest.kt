package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BandWeekTest {
    // The week of Tuesday 27 October 2026 to Sunday 1 November 2026, then the next one.
    private val thisWeek = BandWeek(LocalDate(2026, 10, 27), LocalDate(2026, 11, 1))
    private val nextWeek = BandWeek(LocalDate(2026, 11, 3), LocalDate(2026, 11, 8))

    @Test
    fun `du lundi au dimanche la semaine est celle du dimanche le plus proche`() {
        assertEquals(thisWeek, BandWeek.startingFrom(LocalDate(2026, 10, 26))) // lundi
        assertEquals(thisWeek, BandWeek.startingFrom(LocalDate(2026, 10, 27))) // mardi
        assertEquals(thisWeek, BandWeek.startingFrom(LocalDate(2026, 10, 28))) // mercredi
        assertEquals(thisWeek, BandWeek.startingFrom(LocalDate(2026, 10, 29))) // jeudi
        assertEquals(thisWeek, BandWeek.startingFrom(LocalDate(2026, 10, 30))) // vendredi
        assertEquals(thisWeek, BandWeek.startingFrom(LocalDate(2026, 10, 31))) // samedi
        assertEquals(thisWeek, BandWeek.startingFrom(LocalDate(2026, 11, 1))) // dimanche, jour du service
    }

    @Test
    fun `le lendemain du service on passe a la semaine suivante`() {
        assertEquals(nextWeek, BandWeek.startingFrom(LocalDate(2026, 11, 2)))
    }

    @Test
    fun `la repetition peut tomber le mois ou l annee d avant`() {
        // Friday 1 January 2027: service on Sunday 3, rehearsal on Tuesday 29 December 2026.
        assertEquals(
            BandWeek(LocalDate(2026, 12, 29), LocalDate(2027, 1, 3)),
            BandWeek.startingFrom(LocalDate(2027, 1, 1)),
        )
    }

    @Test
    fun `le service peut tomber le mois ou l annee d apres`() {
        // Monday 28 December 2026: rehearsal on Tuesday 29, service on Sunday 3 January 2027.
        assertEquals(
            BandWeek(LocalDate(2026, 12, 29), LocalDate(2027, 1, 3)),
            BandWeek.startingFrom(LocalDate(2026, 12, 28)),
        )
        // Thursday 26 February 2026: service on Sunday 1 March.
        assertEquals(
            BandWeek(LocalDate(2026, 2, 24), LocalDate(2026, 3, 1)),
            BandWeek.startingFrom(LocalDate(2026, 2, 26)),
        )
    }

    @Test
    fun `la semaine suivante decale les deux dates d une semaine`() {
        assertEquals(nextWeek, thisWeek.next())
    }

    @Test
    fun `la prochaine repetition est le prochain mardi, aujourd hui compris`() {
        assertEquals(LocalDate(2026, 10, 27), BandWeek.nextRehearsal(LocalDate(2026, 10, 27)))
        assertEquals(LocalDate(2026, 11, 3), BandWeek.nextRehearsal(LocalDate(2026, 10, 28)))
        assertEquals(LocalDate(2026, 11, 3), BandWeek.nextRehearsal(LocalDate(2026, 10, 30)))
        assertEquals(LocalDate(2026, 11, 3), BandWeek.nextRehearsal(LocalDate(2026, 11, 2)))
    }
}
