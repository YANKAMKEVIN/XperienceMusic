package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.repository.PlanningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class InMemoryPlanningRepository(
    months: List<PlanningMonth> = emptyList(),
    signups: List<ServiceSignup> = emptyList(),
) : PlanningRepository {
    private val months = MutableStateFlow(months)
    private val signups = MutableStateFlow(signups)

    override fun observeMonths(): Flow<List<PlanningMonth>> = months

    override fun observeSignups(): Flow<List<ServiceSignup>> = signups

    override suspend fun openMonth(monthStart: LocalDate, answerBy: LocalDate?) {
        months.update { list -> list.filterNot { it.monthStart == monthStart } + PlanningMonth(monthStart, answerBy, false) }
    }

    override suspend fun validateMonth(monthStart: LocalDate) {
        months.update { list -> list.map { if (it.monthStart == monthStart) it.copy(isValidated = true) else it } }
    }

    override suspend fun answer(memberId: String, serviceDate: LocalDate, serves: Boolean) {
        signups.update { list ->
            list.filterNot { it.memberId == memberId && it.serviceDate == serviceDate } + ServiceSignup(serviceDate, memberId, serves)
        }
    }
}
