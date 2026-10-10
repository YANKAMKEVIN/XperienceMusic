package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCurrentMemberUseCase(private val sessions: SessionRepository) {
    operator fun invoke(): Flow<Member?> = sessions.observeSession().map { (it as? SessionState.Ready)?.member }
}
