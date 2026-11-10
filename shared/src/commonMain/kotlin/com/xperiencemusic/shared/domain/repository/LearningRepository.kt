package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.domain.model.LearningProgress
import kotlinx.coroutines.flow.Flow

/** The guided path's attempts: written on the phone first, then sent to the server when it answers. */
interface LearningRepository {
    fun observe(memberId: String): Flow<LearningProgress>

    suspend fun record(attempt: LearningAttempt)

    /** Fetches the server's history and sends what waits; a failure leaves the attempts pending. */
    suspend fun sync(memberId: String)
}
