package com.xperiencemusic.shared.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.xperiencemusic.shared.presentation.common.FormattingWords
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.format_decimal_separator
import com.xperiencemusic.shared.resources.format_months
import com.xperiencemusic.shared.resources.format_months_short
import com.xperiencemusic.shared.resources.format_unit_kilobytes
import com.xperiencemusic.shared.resources.format_unit_megabytes
import com.xperiencemusic.shared.resources.format_weekdays
import com.xperiencemusic.shared.resources.format_weekdays_short
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/** The words of dates and sizes, from the string resources, for a screen to spell its values with. */
@Composable
fun rememberFormattingWords(): FormattingWords {
    val weekdays = stringArrayResource(Res.array.format_weekdays)
    val shortWeekdays = stringArrayResource(Res.array.format_weekdays_short)
    val months = stringArrayResource(Res.array.format_months)
    val shortMonths = stringArrayResource(Res.array.format_months_short)
    val decimalSeparator = stringResource(Res.string.format_decimal_separator)
    val kilobytes = stringResource(Res.string.format_unit_kilobytes)
    val megabytes = stringResource(Res.string.format_unit_megabytes)
    return remember(weekdays, shortWeekdays, months, shortMonths, decimalSeparator, kilobytes, megabytes) {
        FormattingWords(weekdays, shortWeekdays, months, shortMonths, decimalSeparator, kilobytes, megabytes)
    }
}
