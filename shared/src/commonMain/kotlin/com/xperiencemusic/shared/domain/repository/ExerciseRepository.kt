package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeSingerExercises(): Flow<List<Exercise>>

    /** Status per exercise id for [memberId]; missing ids are [ExerciseStatus.TODO]. */
    fun observeStatuses(memberId: String): Flow<Map<String, ExerciseStatus>>

    suspend fun setStatus(memberId: String, exerciseId: String, status: ExerciseStatus)
}
