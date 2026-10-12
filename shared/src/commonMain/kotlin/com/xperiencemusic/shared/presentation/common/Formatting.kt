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

    /** "dim. 19 oct." */
    fun shortDayAndDate(date: LocalDate): String =
        "${days[date.dayOfWeek.ordinal].take(3)}. ${date.day} ${months[date.month.ordinal]}"

    /** "13 – 18 oct.", or "28 oct. – 2 nov." across two months. */
    fun range(start: LocalDate, end: LocalDate): String =
        if (start.month == end.month) "${start.day} – ${end.day} ${months[end.month.ordinal]}"
        else "${start.day} ${months[start.month.ordinal]} – ${end.day} ${months[end.month.ordinal]}"

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
