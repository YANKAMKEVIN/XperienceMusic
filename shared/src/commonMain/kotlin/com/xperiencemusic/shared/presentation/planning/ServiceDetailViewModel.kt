package com.xperiencemusic.shared.presentation.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.monthStart
import com.xperiencemusic.shared.domain.model.serviceKind
import com.xperiencemusic.shared.domain.usecase.AdjustServiceUseCase
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.ObservePlanningUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
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

/** The team of the service on [date]; admins can put people on it or take them off. */
class ServiceDetailViewModel(
    private val date: LocalDate,
    observePlanning: ObservePlanningUseCase,
    observeSchedule: ObserveSetlistScheduleUseCase,
    getToday: GetTodayUseCase,
    private val adjustService: AdjustServiceUseCase,
) : ViewModel() {
    private val today = getToday()
    private val action = MutableStateFlow(ServiceDetailUiState(isLoading = false))

    val state: StateFlow<ServiceDetailUiState> = combine(observePlanning(), observeSchedule(), action) { planning, schedule, act ->
        deriveServiceDetail(planning, schedule, date, today).copy(savingMemberId = act.savingMemberId, error = act.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServiceDetailUiState())

    fun onEvent(event: ServiceDetailEvent) {
        when (event) {
            is ServiceDetailEvent.Toggled -> toggle(event.memberId, event.serves)
            ServiceDetailEvent.ErrorShown -> action.update { it.copy(error = null) }
        }
    }

    private fun toggle(memberId: String, serves: Boolean) {
        if (action.value.savingMemberId != null) return
        action.update { it.copy(savingMemberId = memberId, error = null) }
        viewModelScope.launch {
            val result = adjustService(memberId, date, serves)
            action.update { it.copy(savingMemberId = null, error = (result as? DomainResult.Failure)?.error?.toPlanningError()) }
        }
    }
}

fun deriveServiceDetail(planning: Planning, schedule: SetlistSchedule, date: LocalDate, today: LocalDate): ServiceDetailUiState {
    val me = planning.me
    val card = serviceCards(date.monthStart(), planning.members, planning.signups, me?.id, today).firstOrNull { it.date == date }
        ?: return ServiceDetailUiState(isLoading = false, date = date, title = Formatting.dayAndDate(date))
    val canPlan = me?.role?.can(Permission.PLAN_SERVICES) == true
    val onboarded = planning.members.filter { it.displayName != null }
    return ServiceDetailUiState(
        isLoading = false,
        date = date,
        kind = date.serviceKind() ?: card.kind,
        title = Formatting.dayAndDate(date),
        serving = card.serving,
        missing = card.missing,
        songTitles = schedule.weeks.firstOrNull { it.setlist.serviceDate == date }?.songs?.map { it.title }.orEmpty(),
        roster = if (canPlan) {
            onboarded.sortedWith(compareBy({ it.instrument?.ordinal ?: Int.MAX_VALUE }, { it.voicePart?.ordinal ?: 0 }, { it.displayName }))
                .map { member ->
                    RosterRowUi(
                        ServingUi(member.id, member.displayName.orEmpty(), member.instrument, member.voicePart, member.id == me?.id),
                        planning.signups.firstOrNull { it.serviceDate == date && it.memberId == member.id }?.serves,
                    )
                }
        } else {
            emptyList()
        },
    )
}
