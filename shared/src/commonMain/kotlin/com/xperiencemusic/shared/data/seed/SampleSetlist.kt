package com.xperiencemusic.shared.data.seed

import com.xperiencemusic.shared.domain.model.BandWeek
import com.xperiencemusic.shared.domain.model.Setlist
import kotlinx.datetime.LocalDate

internal object SampleSetlist {

    fun forWeekOf(today: LocalDate): Setlist {
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
