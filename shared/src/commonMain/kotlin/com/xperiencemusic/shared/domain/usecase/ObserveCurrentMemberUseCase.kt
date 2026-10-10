package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

class ObserveCurrentMemberUseCase(private val sessions: SessionRepository) {
    operator fun invoke(): Flow<Member?> = sessions.observeCurrentMember()
}
