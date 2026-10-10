package com.xperiencemusic.shared.presentation.common

import kotlinx.datetime.LocalDate

/** The band is French-speaking; dates are spelled in French until the app needs another locale. */
internal object Formatting {
    private val days = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")
    private val months = listOf(
        "janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc.",
    )

    /** "mardi 14 oct." */
    fun dayAndDate(date: LocalDate): String =
        "${days[date.dayOfWeek.ordinal]} ${date.day} ${months[date.month.ordinal]}"

    /** "14 oct. 2026" */
    fun shortDate(date: LocalDate): String = "${date.day} ${months[date.month.ordinal]} ${date.year}"

    /** "4,2 Mo", "820 Ko" */
    fun fileSize(bytes: Long): String {
        val kilo = 1024.0
        return if (bytes >= kilo * kilo) {
            val tenths = (bytes / (kilo * kilo) * 10).toLong()
            "${tenths / 10},${tenths % 10} Mo"
        } else {
            "${(bytes / kilo).toLong().coerceAtLeast(1)} Ko"
        }
    }

    /** "3:32" */
    fun duration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
