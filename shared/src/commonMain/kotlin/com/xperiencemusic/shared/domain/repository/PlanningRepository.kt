package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.ServiceSignup
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Who serves each Friday and Sunday. Server-side, members only answer for themselves. */
interface PlanningRepository {
    fun observeMonths(): Flow<List<PlanningMonth>>

    fun observeSignups(): Flow<List<ServiceSignup>>

    suspend fun openMonth(monthStart: LocalDate, answerBy: LocalDate?)

    suspend fun validateMonth(monthStart: LocalDate)

    /** Records [memberId]'s answer for [serviceDate], replacing an earlier one. */
    suspend fun answer(memberId: String, serviceDate: LocalDate, serves: Boolean)
}
