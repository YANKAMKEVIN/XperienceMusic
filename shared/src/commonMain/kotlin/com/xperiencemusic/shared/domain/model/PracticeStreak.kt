package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus

/**
 * Days of practice in a row up to [today]. One day off in any seven does not break it, so a busy
 * Sunday costs nothing; today still counts as open until it is over.
 */
fun practiceStreak(days: Set<LocalDate>, today: LocalDate): Int {
    val earliest = days.minOrNull() ?: return 0
    var day = today
    var count = 0
    var lastMiss: LocalDate? = null
    while (day >= earliest) {
        if (day in days) {
            count++
        } else if (day != today) {
            if (lastMiss != null && day.daysUntil(lastMiss) < DAYS_PER_WEEK) break
            lastMiss = day
        }
        day = day.minus(DatePeriod(days = 1))
    }
    return count
}

private const val DAYS_PER_WEEK = 7
