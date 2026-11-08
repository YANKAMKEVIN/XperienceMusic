package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** The band's rhythm: rehearsal on Tuesday, service the following Sunday. */
data class BandWeek(val rehearsalDate: LocalDate, val serviceDate: LocalDate) {
    fun next(): BandWeek = BandWeek(rehearsalDate.plus(ONE_WEEK), serviceDate.plus(ONE_WEEK))

    companion object {
        /**
         * The week of the coming service: the nearest Sunday, today included. From Wednesday to
         * Sunday its rehearsal is already behind, but the service still lies ahead.
         */
        fun startingFrom(today: LocalDate): BandWeek {
            val service = today.nextOrSame(DayOfWeek.SUNDAY)
            return BandWeek(service.minus(REHEARSAL_TO_SERVICE), service)
        }

        /** The next rehearsal still to come, today included. */
        fun nextRehearsal(today: LocalDate): LocalDate = today.nextOrSame(DayOfWeek.TUESDAY)

        private fun LocalDate.nextOrSame(day: DayOfWeek): LocalDate =
            plus(DatePeriod(days = (day.ordinal - dayOfWeek.ordinal).mod(DAYS_PER_WEEK)))

        private const val DAYS_PER_WEEK = 7
        private val ONE_WEEK = DatePeriod(days = DAYS_PER_WEEK)
        private val REHEARSAL_TO_SERVICE = DatePeriod(days = 5)
    }
}
