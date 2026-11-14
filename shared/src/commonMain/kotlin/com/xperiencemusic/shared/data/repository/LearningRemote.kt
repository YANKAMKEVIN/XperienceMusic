package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.LearningAttempt

/** Where [FileLearningRepository] sends the attempts kept on the phone. */
interface LearningRemote {
    suspend fun fetch(memberId: String): List<LearningAttempt>

    suspend fun send(attempt: LearningAttempt)
}
