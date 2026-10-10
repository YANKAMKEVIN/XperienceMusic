package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.ExerciseProgress
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** The exercises of a singer's level, in order, with the member's status for each. */
class ObserveSingerPathUseCase(private val exercises: ExerciseRepository) {
    operator fun invoke(memberId: String, level: Level): Flow<List<ExerciseProgress>> =
        combine(exercises.observeSingerExercises(), exercises.observeStatuses(memberId)) { all, statuses ->
            all.filter { it.level == level }
                .sortedBy { it.order }
                .map { ExerciseProgress(it, statuses[it.id] ?: ExerciseStatus.TODO) }
        }
}
