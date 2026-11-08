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
    /** The deadline is behind us but the month is still open: answers are late, not closed. */
    val isPastDeadline: Boolean = false,
    /** Once validated: the services still to come whose team lacks a part, said in the banner. */
    val incompleteAfterValidation: Int = 0,
    /** How many of the month's services the signed-in member has not answered yet: dates, not people. */
    val unansweredCount: Int = 0,
    val services: List<ServiceCardUi> = emptyList(),
    val canPlan: Boolean = false,
    /** For an admin, while the month is open: what validating it would leave unanswered or uncovered. */
    val validation: MonthValidationUi? = null,
    /** "Valider le mois ?" is on screen. */
    val isConfirmingValidation: Boolean = false,
    val isWorking: Boolean = false,
    val error: PlanningError? = null,
)

/** The month's services still to come, as they stand before the admin validates the month. */
data class MonthValidationUi(
    /** One per member and service with no answer yet. */
    val missingAnswers: Int,
    /** The members those answers are missing from. */
    val missingMembers: Int = 0,
    /** Services whose team still lacks a part. */
    val incompleteServices: Int,
    /** The parts missing on at least one of them. */
    val uncoveredParts: List<Instrument>,
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
    val isToday: Boolean = false,
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
    /** Opens the confirmation; validating cannot be undone by the members. */
    data object ValidateMonthClicked : PlanningEvent
    data object ValidateMonthConfirmed : PlanningEvent
    data object ValidateMonthDismissed : PlanningEvent
    data object ErrorShown : PlanningEvent
}
