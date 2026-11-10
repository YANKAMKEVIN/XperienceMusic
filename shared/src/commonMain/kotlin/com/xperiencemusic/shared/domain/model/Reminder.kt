package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * A notification the phone shows at [at], local time; one already due is shown straight away.
 * The domain says what it is about; its words are chosen where it is shown.
 */
sealed interface Reminder {
    /** Stable from one plan to the next, so a reminder planned again replaces itself. */
    val id: String
    val at: LocalDateTime

    /** The month was validated: the member serves on [services]. Announced once. */
    data class MonthValidated(val month: LocalDate, val services: List<LocalDate>, override val at: LocalDateTime) : Reminder {
        override val id: String get() = "validated-$month"
    }

    /** The eve of Tuesday's rehearsal, for the Sunday the member serves. */
    data class RehearsalEve(val sunday: LocalDate, val songTitles: List<String>, override val at: LocalDateTime) : Reminder {
        override val id: String get() = "rehearsal-$sunday"
    }

    /** The eve of a service the member serves. */
    data class ServiceEve(val date: LocalDate, val kind: ServiceKind, override val at: LocalDateTime) : Reminder {
        override val id: String get() = "eve-$date"
    }

    /** The day before the answers are due, while [missing] services have no answer from the member. */
    data class AnswersDue(
        val month: LocalDate,
        val missing: Int,
        val answerBy: LocalDate,
        override val at: LocalDateTime,
    ) : Reminder {
        override val id: String get() = "answers-$month"
    }
}
