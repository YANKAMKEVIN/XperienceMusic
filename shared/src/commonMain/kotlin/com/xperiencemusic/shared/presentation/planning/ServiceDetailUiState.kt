package com.xperiencemusic.shared.presentation.planning

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.ServiceKind
import kotlinx.datetime.LocalDate

data class ServiceDetailUiState(
    val isLoading: Boolean = true,
    val date: LocalDate? = null,
    val kind: ServiceKind = ServiceKind.SUNDAY,
    /** "dimanche 1 nov." */
    val title: String = "",
    val serving: List<ServingUi> = emptyList(),
    val missing: List<Instrument> = emptyList(),
    /** Sundays only: the songs of that service's setlist, once published. */
    val songTitles: List<String> = emptyList(),
    /** Admins: everyone in the band, to put on or take off the team. */
    val roster: List<RosterRowUi> = emptyList(),
    val savingMemberId: String? = null,
    val error: PlanningError? = null,
)

data class RosterRowUi(val member: ServingUi, val serves: Boolean?)

sealed interface ServiceDetailEvent {
    data class Toggled(val memberId: String, val serves: Boolean) : ServiceDetailEvent
    data object ErrorShown : ServiceDetailEvent
}
