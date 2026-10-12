package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface ExerciseRepository {
    fun observeSingerExercises(): Flow<List<Exercise>>

    /** Status per exercise id for [memberId]; missing ids are [ExerciseStatus.TODO]. */
    fun observeStatuses(memberId: String): Flow<Map<String, ExerciseStatus>>

    suspend fun setStatus(memberId: String, exerciseId: String, status: ExerciseStatus)

    /** The days [memberId] practised (an exercise or the warm-up done), for the streak. */
    fun observePracticeDays(memberId: String): Flow<Set<LocalDate>>

    suspend fun recordPractice(memberId: String, day: LocalDate)
}
