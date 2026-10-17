package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.remote.dto.GrowthReviewDto
import com.xperiencemusic.shared.data.remote.dto.LevelRequestDto
import com.xperiencemusic.shared.data.remote.dto.LevelRequestUpsertDto
import com.xperiencemusic.shared.data.remote.dto.ObjectiveCheckDto
import com.xperiencemusic.shared.data.remote.dto.ObjectiveCheckUpsertDto
import com.xperiencemusic.shared.data.remote.ignoringFailures
import com.xperiencemusic.shared.data.remote.postgrestCall
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.LevelRequest
import com.xperiencemusic.shared.domain.model.ObjectiveCheck
import com.xperiencemusic.shared.domain.repository.GrowthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlin.time.Clock

/** Row level security decides what comes back: a member gets their own rows, the super admin everyone's. */
class SupabaseGrowthRepository(private val client: SupabaseClient,
    private val loads: com.xperiencemusic.shared.data.remote.RemoteLoadMonitor = com.xperiencemusic.shared.data.remote.RemoteLoadMonitor()) : GrowthRepository, com.xperiencemusic.shared.domain.repository.SessionResettable {
    private var sessionGeneration = 0

    override fun resetSession() {
        sessionGeneration++
        requests.value = null
        checks.value = null
    }

    private val requests = MutableStateFlow<List<LevelRequest>?>(null)
    private val checks = MutableStateFlow<List<ObjectiveCheck>?>(null)

    override fun observeRequests(): Flow<List<LevelRequest>> = requests.onStart { loads.load("Growth") { reload() } }.filterNotNull()

    override fun observeChecks(): Flow<List<ObjectiveCheck>> = checks.onStart { loads.load("Growth") { reload() } }.filterNotNull()

    override suspend fun requestLevel(memberId: String, target: Level) {
        accountCall {
            client.from(REQUESTS).upsert(LevelRequestUpsertDto(memberId, target.name, now())) { onConflict = "member_id,target_level" }
        }
        reload()
    }

    override suspend fun reviewRequest(requestId: String, status: ExerciseStatus, feedback: String?) {
        accountCall { client.from(REQUESTS).update(GrowthReviewDto(status.name, feedback, now())) { filter { eq("id", requestId) } } }
        reload()
    }

    override suspend fun tickObjective(memberId: String, objectiveId: String) {
        accountCall {
            client.from(CHECKS).upsert(ObjectiveCheckUpsertDto(memberId, objectiveId, now())) { onConflict = "member_id,objective_id" }
        }
        reload()
    }

    override suspend fun untickObjective(memberId: String, objectiveId: String) {
        accountCall {
            client.from(CHECKS).delete {
                filter {
                    eq("member_id", memberId)
                    eq("objective_id", objectiveId)
                }
            }
        }
        reload()
    }

    override suspend fun reviewObjective(memberId: String, objectiveId: String, status: ExerciseStatus, feedback: String?) {
        accountCall {
            client.from(CHECKS).update(GrowthReviewDto(status.name, feedback, now())) {
                filter {
                    eq("member_id", memberId)
                    eq("objective_id", objectiveId)
                }
            }
        }
        reload()
    }

    private suspend fun reload() {
        requests.value = accountCall {
            client.from(REQUESTS).select(Columns.raw(LevelRequestDto.COLUMNS)) { order("submitted_at", Order.DESCENDING) }
                .decodeList<LevelRequestDto>()
        }.map { it.toDomain() }
        checks.value = accountCall {
            client.from(CHECKS).select(Columns.raw(ObjectiveCheckDto.COLUMNS)) { order("submitted_at", Order.DESCENDING) }
                .decodeList<ObjectiveCheckDto>()
        }.map { it.toDomain() }
    }

    private fun now() = Clock.System.now().toString()

    private companion object {
        const val REQUESTS = "level_requests"
        const val CHECKS = "objective_checks"
    }
    /** Discard responses started under a previous account, including snapshot writes. */
    private suspend fun <T> accountCall(block: suspend () -> T): T {
        val generation = sessionGeneration
        val result = postgrestCall(block)
        if (generation != sessionGeneration) throw kotlin.coroutines.cancellation.CancellationException("Session ended")
        return result
    }

}
