package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.remote.dto.ServiceMonthDto
import com.xperiencemusic.shared.data.remote.dto.ServiceMonthOpenDto
import com.xperiencemusic.shared.data.remote.dto.ServiceMonthValidateDto
import com.xperiencemusic.shared.data.remote.dto.ServiceSignupDto
import com.xperiencemusic.shared.data.remote.dto.ServiceSignupUpsertDto
import com.xperiencemusic.shared.data.remote.ignoringFailures
import com.xperiencemusic.shared.data.remote.postgrestCall
import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.repository.PlanningRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

/** `service_months` and `service_signups`; the RLS policies keep members to their own answers. */
class SupabasePlanningRepository(private val client: SupabaseClient) : PlanningRepository {
    private val months = MutableStateFlow<List<PlanningMonth>?>(null)
    private val signups = MutableStateFlow<List<ServiceSignup>?>(null)

    override fun observeMonths(): Flow<List<PlanningMonth>> =
        months.onStart { ignoringFailures { reload() } }.filterNotNull()

    override fun observeSignups(): Flow<List<ServiceSignup>> =
        signups.onStart { ignoringFailures { reload() } }.filterNotNull()

    override suspend fun openMonth(monthStart: LocalDate, answerBy: LocalDate?) {
        postgrestCall { client.from(MONTHS).insert(ServiceMonthOpenDto(monthStart.toString(), answerBy?.toString())) }
        reload()
    }

    override suspend fun validateMonth(monthStart: LocalDate) {
        postgrestCall {
            client.from(MONTHS).update(ServiceMonthValidateDto(Clock.System.now().toString())) {
                filter { eq("month", monthStart.toString()) }
            }
        }
        reload()
    }

    override suspend fun answer(memberId: String, serviceDate: LocalDate, serves: Boolean) {
        postgrestCall {
            client.from(SIGNUPS).upsert(
                ServiceSignupUpsertDto(serviceDate.toString(), memberId, serves, Clock.System.now().toString()),
            ) { onConflict = "service_date,member_id" }
        }
        reload()
    }

    private suspend fun reload() {
        months.value = postgrestCall { client.from(MONTHS).select().decodeList<ServiceMonthDto>() }
            .map { PlanningMonth(LocalDate.parse(it.month), it.answerBy?.let(LocalDate::parse), it.validatedAt != null) }
        signups.value = postgrestCall { client.from(SIGNUPS).select().decodeList<ServiceSignupDto>() }
            .map { ServiceSignup(LocalDate.parse(it.serviceDate), it.memberId, it.serves) }
    }

    private companion object {
        const val MONTHS = "service_months"
        const val SIGNUPS = "service_signups"
    }
}
