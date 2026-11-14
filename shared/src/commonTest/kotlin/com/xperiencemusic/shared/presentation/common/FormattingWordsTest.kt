package com.xperiencemusic.shared.presentation.common

import com.xperiencemusic.shared.fakes.frenchWords
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FormattingWordsTest {
    private val tuesday = LocalDate(2026, 10, 13)
    private val sunday = LocalDate(2026, 10, 18)

    @Test
    fun datesAreSpelledWithTheWordsGiven() {
        assertEquals("mardi 13 oct.", frenchWords.dayAndDate(tuesday))
        assertEquals("dim. 18 oct.", frenchWords.shortDayAndDate(sunday))
        assertEquals("dim.", frenchWords.shortWeekday(sunday))
        assertEquals("18 oct.", frenchWords.dayAndMonth(sunday))
        assertEquals("octobre", frenchWords.month(sunday))
        assertEquals("octobre 2026", frenchWords.monthAndYear(sunday))
        assertEquals("13 oct. 2026", frenchWords.shortDate(tuesday))
    }

    @Test
    fun aRangeNamesTheMonthOnceUnlessItChanges() {
        assertEquals("13 – 18 oct.", frenchWords.range(tuesday, sunday))
        assertEquals("27 oct. – 1 nov.", frenchWords.range(LocalDate(2026, 10, 27), LocalDate(2026, 11, 1)))
    }

    @Test
    fun sizesUseMegabytesFromOneMegabyte() {
        assertEquals("1,5 Mo", frenchWords.fileSize(3L * 1024 * 1024 / 2))
        assertEquals("820 Ko", frenchWords.fileSize(820L * 1024))
        assertEquals("1 Ko", frenchWords.fileSize(10))
    }

    @Test
    fun durationsNeedNoWords() {
        assertEquals("3:32", Formatting.duration(212))
        assertEquals("0:05", Formatting.duration(5))
    }

    @Test
    fun incompleteWordsAreRefused() {
        assertFailsWith<IllegalArgumentException> { frenchWords.copy(months = frenchWords.months.drop(1)) }
    }
}
