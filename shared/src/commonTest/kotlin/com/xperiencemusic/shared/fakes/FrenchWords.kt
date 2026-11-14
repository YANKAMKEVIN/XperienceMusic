package com.xperiencemusic.shared.fakes

import com.xperiencemusic.shared.presentation.common.FormattingWords

/** The words of `values/strings.xml` (`format_*`), for tests, which run without the resources. */
val frenchWords = FormattingWords(
    weekdays = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"),
    shortWeekdays = listOf("lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim."),
    months = listOf(
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre",
    ),
    shortMonths = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."),
    decimalSeparator = ",",
    kilobytes = "Ko",
    megabytes = "Mo",
)
