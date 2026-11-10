package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.remote.RemoteLoadMonitor
import com.xperiencemusic.shared.data.remote.dto.ServiceMonthDto
import com.xperiencemusic.shared.data.remote.dto.ServiceMonthOpenDto
import com.xperiencemusic.shared.data.remote.dto.ServiceMonthValidateDto
import com.xperiencemusic.shared.data.remote.dto.ServiceSignupDto
import com.xperiencemusic.shared.data.remote.dto.ServiceSignupUpsertDto
import com.xperiencemusic.shared.data.remote.ignoringFailures
import com.xperiencemusic.shared.data.session.SessionGuard
import com.xperiencemusic.shared.data.session.accountCall
import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.repository.PlanningRepository
import com.xperiencemusic.shared.domain.repository.SessionResettable
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.LocalDate

/** `service_months` and `service_signups`; the RLS policies keep members to their own answers. */
class SupabasePlanningRepository(
    private val client: SupabaseClient,
    private val loads: RemoteLoadMonitor = RemoteLoadMonitor(),
) : PlanningRepository, SessionResettable {
    private val session = SessionGuard()

    override fun resetSession() {
        session.reset()
        months.value = null
        signups.value = null
    }

    private val months = MutableStateFlow<List<PlanningMonth>?>(null)
    private val signups = MutableStateFlow<List<ServiceSignup>?>(null)

    override fun observeMonths(): Flow<List<PlanningMonth>> =
        months.onStart { loads.load("Planning") { reload() } }.filterNotNull()

    override fun observeSignups(): Flow<List<ServiceSignup>> =
        signups.onStart { loads.load("Planning") { reload() } }.filterNotNull()

    override suspend fun openMonth(monthStart: LocalDate, answerBy: LocalDate?) {
        session.accountCall { client.from(MONTHS).insert(ServiceMonthOpenDto(monthStart.toString(), answerBy?.toString())) }
        reload()
    }

    override suspend fun validateMonth(monthStart: LocalDate) {
        session.accountCall {
            client.from(MONTHS).update(ServiceMonthValidateDto(Clock.System.now().toString())) {
                filter { eq("month", monthStart.toString()) }
            }
        }
        reload()
    }

    override suspend fun answer(memberId: String, serviceDate: LocalDate, serves: Boolean) {
        session.accountCall {
            client.from(SIGNUPS).upsert(
                ServiceSignupUpsertDto(serviceDate.toString(), memberId, serves, Clock.System.now().toString()),
            ) { onConflict = "service_date,member_id" }
        }
        reload()
    }

    private suspend fun reload() {
        months.value = session.accountCall { client.from(MONTHS).select().decodeList<ServiceMonthDto>() }
            .map { PlanningMonth(LocalDate.parse(it.month), it.answerBy?.let(LocalDate::parse), it.validatedAt != null) }
        signups.value = session.accountCall { client.from(SIGNUPS).select().decodeList<ServiceSignupDto>() }
            .map { ServiceSignup(LocalDate.parse(it.serviceDate), it.memberId, it.serves) }
    }

    private companion object {
        const val MONTHS = "service_months"
        const val SIGNUPS = "service_signups"
    }
}
