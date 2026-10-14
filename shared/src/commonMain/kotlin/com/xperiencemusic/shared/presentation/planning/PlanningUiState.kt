package com.xperiencemusic.shared.presentation.planning

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.ServiceKind
import com.xperiencemusic.shared.domain.model.VoicePart
import kotlinx.datetime.LocalDate

/** Where a month stands. */
enum class MonthStatus { NOT_OPEN, OPEN, VALIDATED }

/** The signed-in member's answer for one service. */
enum class MyAnswer { SERVES, NOT_AVAILABLE, UNANSWERED }

data class PlanningUiState(
    val isLoading: Boolean = true,
    val monthStart: LocalDate? = null,
    /** "novembre 2026" */
    val monthLabel: String = "",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val status: MonthStatus = MonthStatus.NOT_OPEN,
    /** "25 oct.", while the month is open with a deadline. */
    val answerByLabel: String? = null,
    /** How many of the month's services the signed-in member has not answered yet. */
    val unansweredCount: Int = 0,
    val services: List<ServiceCardUi> = emptyList(),
    val canPlan: Boolean = false,
    val isWorking: Boolean = false,
    val error: PlanningError? = null,
)

data class ServiceCardUi(
    val date: LocalDate,
    val kind: ServiceKind,
    /** "dim." */
    val weekday: String,
    val day: Int,
    val serving: List<ServingUi>,
    val missing: List<Instrument>,
    val myAnswer: MyAnswer,
    val isPast: Boolean,
    /** "À répondre" only makes sense while the member can still answer. */
    val showsAnswer: Boolean = myAnswer != MyAnswer.UNANSWERED,
)

data class ServingUi(
    val memberId: String,
    val name: String,
    val instrument: Instrument?,
    val voicePart: VoicePart?,
    val isMe: Boolean,
)

enum class PlanningError { FORBIDDEN, NETWORK }

sealed interface PlanningEvent {
    data object PreviousMonth : PlanningEvent
    data object NextMonth : PlanningEvent
    data object OpenMonthClicked : PlanningEvent
    data object ValidateMonthClicked : PlanningEvent
    data object ErrorShown : PlanningEvent
}
