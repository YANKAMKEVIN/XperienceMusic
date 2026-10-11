package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.repository.ExerciseRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository

/** Marks an exercise validated for the signed-in member (after an automatic check succeeded). */
class CompleteExerciseUseCase(
    private val sessions: SessionRepository,
    private val exercises: ExerciseRepository,
) {
    suspend operator fun invoke(exerciseId: String): DomainResult<Unit> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        return runRemote { exercises.setStatus(member.id, exerciseId, ExerciseStatus.VALIDATED) }
    }
}
