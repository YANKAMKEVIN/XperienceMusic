package com.xperiencemusic.shared.presentation.planning

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.ServiceKind
import com.xperiencemusic.shared.domain.model.VoicePart
import kotlinx.datetime.LocalDate

data class AvailabilityUiState(
    val isLoading: Boolean = true,
    val monthStart: LocalDate? = null,
    val answerBy: LocalDate? = null,
    /** The deadline has passed; answers are still taken until the month is validated. */
    val isPastDeadline: Boolean = false,
    val instrument: Instrument? = null,
    val voicePart: VoicePart? = null,
    /** False once the month is validated (or was never opened): answers are read-only. */
    val canAnswer: Boolean = false,
    val rows: List<AvailabilityRowUi> = emptyList(),
    val savingDate: LocalDate? = null,
    val error: PlanningError? = null,
)

data class AvailabilityRowUi(
    val date: LocalDate,
    val kind: ServiceKind,
    val answer: MyAnswer,
    val isPast: Boolean,
)

sealed interface AvailabilityEvent {
    data class Answered(val date: LocalDate, val serves: Boolean) : AvailabilityEvent
    data object ErrorShown : AvailabilityEvent
}
