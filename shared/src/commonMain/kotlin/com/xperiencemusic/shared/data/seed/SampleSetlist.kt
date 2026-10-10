package com.xperiencemusic.shared.data.seed

import com.xperiencemusic.shared.domain.model.Setlist
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

internal object SampleSetlist {

    /** This week's Tuesday rehearsal (today if it is Tuesday) and the Sunday after it. */
    fun forWeekOf(today: LocalDate): Setlist {
        val rehearsal = today.nextOrSame(DayOfWeek.TUESDAY)
        return Setlist(
            id = "setlist-$rehearsal",
            rehearsalDate = rehearsal,
            serviceDate = rehearsal.nextOrSame(DayOfWeek.SUNDAY),
            songIds = listOf("a-toi-la-gloire", "holy-holy-holy", "amazing-grace"),
            message = "On reprend « À toi la gloire » avec la version acoustique en intro.",
            publishedBy = "Responsable musical",
        )
    }

    private fun LocalDate.nextOrSame(day: DayOfWeek): LocalDate {
        val daysAhead = (day.ordinal - dayOfWeek.ordinal).mod(7)
        return plus(DatePeriod(days = daysAhead))
    }
}
