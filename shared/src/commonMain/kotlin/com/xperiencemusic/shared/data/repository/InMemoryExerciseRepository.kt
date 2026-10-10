package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryExerciseRepository(private val exercises: List<Exercise>) : ExerciseRepository {
    /** memberId -> (exerciseId -> status) */
    private val statuses = MutableStateFlow<Map<String, Map<String, ExerciseStatus>>>(emptyMap())

    override fun observeSingerExercises(): Flow<List<Exercise>> = flowOf(exercises)

    override fun observeStatuses(memberId: String): Flow<Map<String, ExerciseStatus>> =
        statuses.map { it[memberId].orEmpty() }

    override suspend fun setStatus(memberId: String, exerciseId: String, status: ExerciseStatus) {
        statuses.update { all ->
            all + (memberId to (all[memberId].orEmpty() + (exerciseId to status)))
        }
    }
}
