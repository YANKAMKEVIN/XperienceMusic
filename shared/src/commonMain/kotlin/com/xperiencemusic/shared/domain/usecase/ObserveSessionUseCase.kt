package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

class ObserveSessionUseCase(private val sessions: SessionRepository) {
    operator fun invoke(): Flow<SessionState> = sessions.observeSession()
}
