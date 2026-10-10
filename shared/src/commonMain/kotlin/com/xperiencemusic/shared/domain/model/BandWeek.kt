package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** The band's rhythm: rehearsal on Tuesday, service the following Sunday. */
data class BandWeek(val rehearsalDate: LocalDate, val serviceDate: LocalDate) {
    companion object {
        /** The coming week, counting today when it is Tuesday. */
        fun startingFrom(today: LocalDate): BandWeek {
            val rehearsal = today.nextOrSame(DayOfWeek.TUESDAY)
            return BandWeek(rehearsal, rehearsal.nextOrSame(DayOfWeek.SUNDAY))
        }

        private fun LocalDate.nextOrSame(day: DayOfWeek): LocalDate =
            plus(DatePeriod(days = (day.ordinal - dayOfWeek.ordinal).mod(DAYS_PER_WEEK)))

        private const val DAYS_PER_WEEK = 7
    }
}
