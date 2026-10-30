package com.xperiencemusic.shared.domain.learning

import kotlinx.coroutines.flow.Flow

interface LearningRepository {
    fun observe(memberId: String): Flow<LearningProgress>
    suspend fun record(attempt: LearningAttempt)
    suspend fun sync(memberId: String)
}
