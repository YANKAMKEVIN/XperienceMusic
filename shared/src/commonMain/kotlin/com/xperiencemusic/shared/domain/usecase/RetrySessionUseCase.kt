package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.repository.SessionRepository

class RetrySessionUseCase(private val sessions: SessionRepository) {
    suspend operator fun invoke(): DomainResult<Unit> = runRemote { sessions.refresh() }
}
