package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.ExerciseProgress
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.SingerPath
import com.xperiencemusic.shared.domain.model.practiceStreak
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** The singer's path with the member's status for each step and their practice streak. */
class ObserveSingerPathUseCase(
    private val exercises: ExerciseRepository,
    private val dates: DateProvider,
) {
    operator fun invoke(memberId: String): Flow<SingerPath> =
        combine(
            exercises.observeSingerExercises(),
            exercises.observeStatuses(memberId),
            exercises.observePracticeDays(memberId),
        ) { all, statuses, days ->
            val (warmUps, path) = all.partition { it.tier == Exercise.WARM_UP_TIER }
            val today = dates.today()
            SingerPath.of(
                path.map { ExerciseProgress(it, statuses[it.id] ?: ExerciseStatus.TODO) },
                warmUp = warmUps.firstOrNull(),
                streakDays = practiceStreak(days, today),
                hasPractisedToday = today in days,
            )
        }
}
