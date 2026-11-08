package com.xperiencemusic.shared.presentation.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.monthStart
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.ObservePlanningUseCase
import com.xperiencemusic.shared.domain.usecase.OpenPlanningMonthUseCase
import com.xperiencemusic.shared.domain.usecase.ValidatePlanningMonthUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

class PlanningViewModel(
    observePlanning: ObservePlanningUseCase,
    getToday: GetTodayUseCase,
    private val openMonth: OpenPlanningMonthUseCase,
    private val validateMonth: ValidatePlanningMonthUseCase,
) : ViewModel() {
    private val today = getToday()
    private val selectedMonth = MutableStateFlow(today.monthStart())
    private val action = MutableStateFlow(ActionState())
    private val isConfirmingValidation = MutableStateFlow(false)
    private var shownMonths: List<LocalDate> = emptyList()

    val state: StateFlow<PlanningUiState> = combine(observePlanning(), selectedMonth, action, isConfirmingValidation) { planning, month, act, confirming ->
        shownMonths = planningMonths(today, planning.months, planning.me?.role?.can(Permission.PLAN_SERVICES) == true)
        derivePlanningState(planning, month, today).let {
            it.copy(isWorking = act.isWorking, error = act.error, isConfirmingValidation = confirming && it.validation != null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanningUiState())

    fun onEvent(event: PlanningEvent) {
        when (event) {
            PlanningEvent.PreviousMonth -> shownMonths.lastOrNull { it < selectedMonth.value }?.let { selectedMonth.value = it }
            PlanningEvent.NextMonth -> shownMonths.firstOrNull { it > selectedMonth.value }?.let { selectedMonth.value = it }
            // Answers are due a week before the month starts.
            PlanningEvent.OpenMonthClicked -> run {
                val month = selectedMonth.value
                val answerBy = month.minus(DatePeriod(days = 7)).takeIf { it > today }
                openMonth(month, answerBy)
            }
            PlanningEvent.ValidateMonthClicked -> isConfirmingValidation.value = true
            PlanningEvent.ValidateMonthDismissed -> isConfirmingValidation.value = false
            PlanningEvent.ValidateMonthConfirmed -> {
                isConfirmingValidation.value = false
                run { validateMonth(selectedMonth.value) }
            }
            PlanningEvent.ErrorShown -> action.update { it.copy(error = null) }
        }
    }

    private fun run(block: suspend () -> DomainResult<Unit>) {
        if (action.value.isWorking) return
        action.value = ActionState(isWorking = true)
        viewModelScope.launch {
            val result = block()
            action.value = ActionState(error = (result as? DomainResult.Failure)?.error?.toPlanningError())
        }
    }

    private data class ActionState(val isWorking: Boolean = false, val error: PlanningError? = null)
}

internal fun DomainError.toPlanningError() = when (this) {
    DomainError.FORBIDDEN, DomainError.NOT_SIGNED_IN -> PlanningError.FORBIDDEN
    else -> PlanningError.NETWORK
}
