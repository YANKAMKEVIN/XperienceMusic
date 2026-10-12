package com.xperiencemusic.shared.data.seed

import com.xperiencemusic.shared.domain.model.BandWeek
import com.xperiencemusic.shared.domain.model.Setlist
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

internal object SampleSetlist {

    /** This week's setlist and the next one, sent ahead. */
    fun around(today: LocalDate): List<Setlist> {
        val next = BandWeek.startingFrom(today.plus(DatePeriod(days = 7)))
        return listOf(
            forWeekOf(today),
            Setlist(
                id = "setlist-${next.rehearsalDate}",
                rehearsalDate = next.rehearsalDate,
                serviceDate = next.serviceDate,
                songIds = listOf("holy-holy-holy", "a-toi-la-gloire"),
                message = null,
                publishedBy = "Responsable musical",
            ),
        )
    }

    private fun forWeekOf(today: LocalDate): Setlist {
        val week = BandWeek.startingFrom(today)
        return Setlist(
            id = "setlist-${week.rehearsalDate}",
            rehearsalDate = week.rehearsalDate,
            serviceDate = week.serviceDate,
            songIds = listOf("a-toi-la-gloire", "holy-holy-holy", "amazing-grace"),
            message = "On reprend « À toi la gloire » avec la version acoustique en intro.",
            publishedBy = "Responsable musical",
        )
    }
}
