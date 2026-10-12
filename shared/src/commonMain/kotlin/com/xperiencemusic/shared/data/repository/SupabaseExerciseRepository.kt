package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.mapper.toExerciseStatus
import com.xperiencemusic.shared.data.remote.dto.ExerciseProgressDto
import com.xperiencemusic.shared.data.remote.dto.PracticeDayDto
import com.xperiencemusic.shared.data.remote.ignoringFailures
import com.xperiencemusic.shared.data.remote.postgrestCall
import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.repository.ExerciseRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer

/** Exercises are app content; only each member's statuses are stored in Supabase. */
class SupabaseExerciseRepository(
    private val client: SupabaseClient,
    private val exercises: List<Exercise>,
    private val snapshots: SnapshotStore,
) : ExerciseRepository {
    /** memberId -> (exerciseId -> status) */
    private val statuses = MutableStateFlow<Map<String, Map<String, ExerciseStatus>>>(emptyMap())

    override fun observeSingerExercises(): Flow<List<Exercise>> = flowOf(exercises)

    override fun observeStatuses(memberId: String): Flow<Map<String, ExerciseStatus>> =
        statuses.onStart {
            if (memberId !in statuses.value) {
                snapshots.load(snapshotOf(memberId), ListSerializer(ExerciseProgressDto.serializer()))?.let { publish(memberId, it) }
                ignoringFailures { reload(memberId) }
            }
        }
            .map { it[memberId] }
            .filterNotNull()

    override suspend fun setStatus(memberId: String, exerciseId: String, status: ExerciseStatus) {
        postgrestCall {
            client.from(PROGRESS).upsert(ExerciseProgressDto(memberId, exerciseId, status.name))
        }
        statuses.update { all -> all + (memberId to (all[memberId].orEmpty() + (exerciseId to status))) }
    }

    private suspend fun reload(memberId: String) {
        val rows = postgrestCall {
            client.from(PROGRESS).select { filter { eq("member_id", memberId) } }.decodeList<ExerciseProgressDto>()
        }
        publish(memberId, rows)
        snapshots.save(snapshotOf(memberId), ListSerializer(ExerciseProgressDto.serializer()), rows)
    }

    private fun publish(memberId: String, rows: List<ExerciseProgressDto>) {
        statuses.update { all -> all + (memberId to rows.associate { it.exerciseId to it.status.toExerciseStatus() }) }
    }

    /** memberId -> days practised; loaded like the statuses, cached for offline use. */
    private val practiceDays = MutableStateFlow<Map<String, Set<LocalDate>>>(emptyMap())

    override fun observePracticeDays(memberId: String): Flow<Set<LocalDate>> =
        practiceDays.onStart {
            if (memberId !in practiceDays.value) {
                snapshots.load(daysSnapshotOf(memberId), ListSerializer(PracticeDayDto.serializer()))
                    ?.let { rows -> practiceDays.update { it + (memberId to rows.map { row -> row.day }.toSet()) } }
                ignoringFailures { reloadDays(memberId) }
            }
        }
            // Nothing saved yet means no practice, not "unknown": the streak starts at 0.
            .map { it[memberId].orEmpty() }

    override suspend fun recordPractice(memberId: String, day: LocalDate) {
        practiceDays.update { all -> all + (memberId to (all[memberId].orEmpty() + day)) }
        postgrestCall {
            client.from(PRACTICE_DAYS).upsert(PracticeDayDto(memberId, day)) { ignoreDuplicates = true }
        }
    }

    private suspend fun reloadDays(memberId: String) {
        val rows = postgrestCall {
            client.from(PRACTICE_DAYS).select { filter { eq("member_id", memberId) } }.decodeList<PracticeDayDto>()
        }
        practiceDays.update { it + (memberId to rows.map { row -> row.day }.toSet()) }
        snapshots.save(daysSnapshotOf(memberId), ListSerializer(PracticeDayDto.serializer()), rows)
    }

    private companion object {
        const val PROGRESS = "exercise_progress"
        const val PRACTICE_DAYS = "practice_days"

        fun snapshotOf(memberId: String) = "progress-$memberId"
        fun daysSnapshotOf(memberId: String) = "practice-$memberId"
    }
}
