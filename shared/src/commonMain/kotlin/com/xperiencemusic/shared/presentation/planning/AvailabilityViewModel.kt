package com.xperiencemusic.shared.presentation.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.usecase.AnswerServiceUseCase
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.ObservePlanningUseCase
import com.xperiencemusic.shared.domain.usecase.Planning
import com.xperiencemusic.shared.presentation.common.Formatting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** The signed-in member's answers for the month starting on [monthStart]; each answer is saved at once. */
class AvailabilityViewModel(
    private val monthStart: LocalDate,
    observePlanning: ObservePlanningUseCase,
    getToday: GetTodayUseCase,
    private val answerService: AnswerServiceUseCase,
) : ViewModel() {
    private val today = getToday()
    private val action = MutableStateFlow(AvailabilityUiState(isLoading = false))

    val state: StateFlow<AvailabilityUiState> = combine(observePlanning(), action) { planning, act ->
        deriveAvailabilityState(planning, monthStart, today).copy(savingDate = act.savingDate, error = act.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AvailabilityUiState())

    fun onEvent(event: AvailabilityEvent) {
        when (event) {
            is AvailabilityEvent.Answered -> answer(event.date, event.serves)
            AvailabilityEvent.ErrorShown -> action.update { it.copy(error = null) }
        }
    }

    private fun answer(date: LocalDate, serves: Boolean) {
        if (action.value.savingDate != null) return
        action.update { it.copy(savingDate = date, error = null) }
        viewModelScope.launch {
            val result = answerService(date, serves)
            action.update { it.copy(savingDate = null, error = (result as? DomainResult.Failure)?.error?.toPlanningError()) }
        }
    }
}

fun deriveAvailabilityState(planning: Planning, monthStart: LocalDate, today: LocalDate): AvailabilityUiState {
    val me = planning.me
    val month = planning.months.firstOrNull { it.monthStart == monthStart }
    val isOpen = month != null && !month.isValidated
    val canPlan = me?.role?.can(Permission.PLAN_SERVICES) == true
    return AvailabilityUiState(
        isLoading = false,
        monthLabel = Formatting.monthAndYear(monthStart),
        answerByLabel = month?.answerBy?.takeIf { isOpen }?.let(Formatting::dayAndMonth),
        instrument = me?.profile?.instrument,
        voicePart = me?.profile?.voicePart,
        canAnswer = month != null && (isOpen || canPlan),
        rows = serviceCards(monthStart, planning.members, planning.signups, me?.id, today).map { card ->
            AvailabilityRowUi(card.date, card.kind, Formatting.dayAndDate(card.date), card.myAnswer, card.isPast)
        },
    )
}
