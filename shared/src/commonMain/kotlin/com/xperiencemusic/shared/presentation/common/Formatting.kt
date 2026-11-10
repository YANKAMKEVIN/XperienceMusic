package com.xperiencemusic.shared.presentation.common

import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.format_decimal_separator
import com.xperiencemusic.shared.resources.format_months
import com.xperiencemusic.shared.resources.format_months_short
import com.xperiencemusic.shared.resources.format_unit_kilobytes
import com.xperiencemusic.shared.resources.format_unit_megabytes
import com.xperiencemusic.shared.resources.format_weekdays
import com.xperiencemusic.shared.resources.format_weekdays_short
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import kotlin.concurrent.Volatile

/**
 * The words dates and file sizes are spelled with. They live in the string resources
 * (`format_*`); [Formatting.loadWords] reads them once the app starts. Until then, and in unit
 * tests that run without resources, [French] stands in: the band is French-speaking, so both
 * read the same today.
 */
internal data class FormattingWords(
    val weekdays: List<String>,
    val shortWeekdays: List<String>,
    val months: List<String>,
    val shortMonths: List<String>,
    val decimalSeparator: String,
    val kilobytes: String,
    val megabytes: String,
) {
    companion object {
        /** A copy of `values/strings.xml`, for the moments before the resources are read. */
        val French = FormattingWords(
            weekdays = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"),
            shortWeekdays = listOf("lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim."),
            months = listOf(
                "janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre",
            ),
            shortMonths = listOf(
                "janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc.",
            ),
            decimalSeparator = ",",
            kilobytes = "Ko",
            megabytes = "Mo",
        )
    }
}

/** Dates, sizes and durations as the UI spells them, with the words of [FormattingWords]. */
internal object Formatting {
    /** Keeps "1 nov." on one line: a day never ends a line with its month on the next. */
    private const val NBSP = " "

    @Volatile
    private var words: FormattingWords = FormattingWords.French

    /**
     * Reads the words from the string resources (the device's language once there are
     * translations). Called once at start-up; keeps the current words if the resources fail.
     */
    suspend fun loadWords() {
        runCatching {
            FormattingWords(
                weekdays = getStringArray(Res.array.format_weekdays),
                shortWeekdays = getStringArray(Res.array.format_weekdays_short),
                months = getStringArray(Res.array.format_months),
                shortMonths = getStringArray(Res.array.format_months_short),
                decimalSeparator = getString(Res.string.format_decimal_separator),
                kilobytes = getString(Res.string.format_unit_kilobytes),
                megabytes = getString(Res.string.format_unit_megabytes),
            )
        }.onSuccess { loaded ->
            if (loaded.weekdays.size == 7 && loaded.shortWeekdays.size == 7 && loaded.months.size == 12 && loaded.shortMonths.size == 12) {
                words = loaded
            }
        }
    }

    private fun weekday(date: LocalDate) = words.weekdays[date.dayOfWeek.ordinal]
    private fun shortMonth(date: LocalDate) = words.shortMonths[date.month.ordinal]

    /** "mardi 14 oct." */
    fun dayAndDate(date: LocalDate): String = "${weekday(date)} ${date.day}$NBSP${shortMonth(date)}"

    /** "dim. 19 oct." */
    fun shortDayAndDate(date: LocalDate): String = "${shortWeekday(date)} ${date.day}$NBSP${shortMonth(date)}"

    /** "13 – 18 oct.", or "28 oct. – 2 nov." across two months. */
    fun range(start: LocalDate, end: LocalDate): String =
        if (start.month == end.month) "${start.day} – ${end.day}$NBSP${shortMonth(end)}"
        else "${start.day}$NBSP${shortMonth(start)} – ${end.day}$NBSP${shortMonth(end)}"

    /** "dim." */
    fun shortWeekday(date: LocalDate): String = words.shortWeekdays[date.dayOfWeek.ordinal]

    /** "25 oct." */
    fun dayAndMonth(date: LocalDate): String = "${date.day}$NBSP${shortMonth(date)}"

    /** "novembre 2026" */
    fun monthAndYear(date: LocalDate): String = "${words.months[date.month.ordinal]} ${date.year}"

    /** "14 oct. 2026" */
    fun shortDate(date: LocalDate): String = "${date.day}$NBSP${shortMonth(date)} ${date.year}"

    /** "4,2 Mo", "820 Ko" */
    fun fileSize(bytes: Long): String {
        val kilo = 1024.0
        return if (bytes >= kilo * kilo) {
            val tenths = (bytes / (kilo * kilo) * 10).toLong()
            "${tenths / 10}${words.decimalSeparator}${tenths % 10} ${words.megabytes}"
        } else {
            "${(bytes / kilo).toLong().coerceAtLeast(1)} ${words.kilobytes}"
        }
    }

    /** "3:32" */
    fun duration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
