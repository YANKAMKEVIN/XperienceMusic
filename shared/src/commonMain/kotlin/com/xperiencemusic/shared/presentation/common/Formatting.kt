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

/**
 * The words dates and file sizes are spelled with. Their only source is the string resources
 * (`format_*`): screens read them with `rememberFormattingWords()`, code outside a composition
 * (the reminders) with [loadFormattingWords]. View models keep dates and sizes as values.
 */
data class FormattingWords(
    /** Monday first, as `DayOfWeek`. */
    val weekdays: List<String>,
    val shortWeekdays: List<String>,
    /** January first, as `Month`. */
    val months: List<String>,
    val shortMonths: List<String>,
    val decimalSeparator: String,
    val kilobytes: String,
    val megabytes: String,
) {
    init {
        require(weekdays.size == 7 && shortWeekdays.size == 7) { "7 weekdays expected" }
        require(months.size == 12 && shortMonths.size == 12) { "12 months expected" }
    }

    private fun weekday(date: LocalDate) = weekdays[date.dayOfWeek.ordinal]
    private fun shortMonth(date: LocalDate) = shortMonths[date.month.ordinal]

    /** "mardi 14 oct." */
    fun dayAndDate(date: LocalDate): String = "${weekday(date)} ${date.day}$NBSP${shortMonth(date)}"

    /** "dim. 19 oct." */
    fun shortDayAndDate(date: LocalDate): String = "${shortWeekday(date)} ${date.day}$NBSP${shortMonth(date)}"

    /** "13 – 18 oct.", or "28 oct. – 2 nov." across two months. */
    fun range(start: LocalDate, end: LocalDate): String =
        if (start.month == end.month) {
            "${start.day} – ${end.day}$NBSP${shortMonth(end)}"
        } else {
            "${start.day}$NBSP${shortMonth(start)} – ${end.day}$NBSP${shortMonth(end)}"
        }

    /** "dim." */
    fun shortWeekday(date: LocalDate): String = shortWeekdays[date.dayOfWeek.ordinal]

    /** "25 oct." */
    fun dayAndMonth(date: LocalDate): String = "${date.day}$NBSP${shortMonth(date)}"

    /** "novembre" */
    fun month(date: LocalDate): String = months[date.month.ordinal]

    /** "novembre 2026" */
    fun monthAndYear(date: LocalDate): String = "${month(date)} ${date.year}"

    /** "14 oct. 2026" */
    fun shortDate(date: LocalDate): String = "${date.day}$NBSP${shortMonth(date)} ${date.year}"

    /** "4,2 Mo", "820 Ko" */
    fun fileSize(bytes: Long): String {
        val kilo = 1024.0
        return if (bytes >= kilo * kilo) {
            val tenths = (bytes / (kilo * kilo) * 10).toLong()
            "${tenths / 10}$decimalSeparator${tenths % 10} $megabytes"
        } else {
            "${(bytes / kilo).toLong().coerceAtLeast(1)} $kilobytes"
        }
    }

    private companion object {
        /** Keeps "1 nov." on one line: a day never ends a line with its month on the next. */
        const val NBSP = " "
    }
}

/** Reads [FormattingWords] from the string resources, for code that runs outside a screen. */
suspend fun loadFormattingWords(): FormattingWords = FormattingWords(
    weekdays = getStringArray(Res.array.format_weekdays),
    shortWeekdays = getStringArray(Res.array.format_weekdays_short),
    months = getStringArray(Res.array.format_months),
    shortMonths = getStringArray(Res.array.format_months_short),
    decimalSeparator = getString(Res.string.format_decimal_separator),
    kilobytes = getString(Res.string.format_unit_kilobytes),
    megabytes = getString(Res.string.format_unit_megabytes),
)

/** Spellings that need no words. */
internal object Formatting {
    /** "3:32" */
    fun duration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
