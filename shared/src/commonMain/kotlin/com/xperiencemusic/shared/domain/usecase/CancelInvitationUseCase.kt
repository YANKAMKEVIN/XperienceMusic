package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.repository.MembersRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository

class CancelInvitationUseCase(
    private val sessions: SessionRepository,
    private val members: MembersRepository,
) {
    suspend operator fun invoke(email: String): DomainResult<Unit> {
        requireMemberManager(sessions).let { if (it is DomainResult.Failure) return it }
        return runRemote { members.cancelInvitation(email) }
    }
}
