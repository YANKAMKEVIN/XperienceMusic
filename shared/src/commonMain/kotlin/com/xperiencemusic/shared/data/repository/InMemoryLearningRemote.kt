package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.LearningAttempt

/** Demo mode: a server that keeps the attempts for the session and always answers. */
class InMemoryLearningRemote : LearningRemote {
    private val attempts = mutableMapOf<String, LearningAttempt>()

    override suspend fun fetch(memberId: String): List<LearningAttempt> = attempts.values.filter { it.memberId == memberId }

    override suspend fun send(attempt: LearningAttempt) {
        attempts[attempt.id] = attempt
    }
}
