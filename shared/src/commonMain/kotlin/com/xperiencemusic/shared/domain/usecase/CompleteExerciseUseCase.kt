package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.ExerciseRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository

/**
 * Records a successful attempt for the signed-in member: the step is validated (the warm-up has no
 * status) and today counts towards the streak.
 */
class CompleteExerciseUseCase(
    private val sessions: SessionRepository,
    private val exercises: ExerciseRepository,
    private val dates: DateProvider,
) {
    suspend operator fun invoke(exercise: Exercise): DomainResult<Unit> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        return runRemote {
            exercises.recordPractice(member.id, dates.today())
            if (exercise.tier != Exercise.WARM_UP_TIER) {
                exercises.setStatus(member.id, exercise.id, ExerciseStatus.VALIDATED)
            }
        }
    }
}
