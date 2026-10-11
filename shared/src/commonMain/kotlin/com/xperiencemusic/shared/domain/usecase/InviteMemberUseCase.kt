package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.repository.MembersRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first

class InviteMemberUseCase(
    private val sessions: SessionRepository,
    private val members: MembersRepository,
) {
    /** Returns the normalized email. */
    suspend operator fun invoke(email: String, role: Role): DomainResult<String> {
        requireMemberManager(sessions).let { if (it is DomainResult.Failure) return it }
        val normalized = email.trim().lowercase()
        if (!EMAIL.matches(normalized)) return DomainResult.Failure(DomainError.INVALID_INPUT)
        if (members.observeInvitations().first().any { it.email == normalized }) {
            return DomainResult.Failure(DomainError.ALREADY_INVITED)
        }
        return runRemote {
            members.invite(normalized, role)
            normalized
        }
    }

    private companion object {
        val EMAIL = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")
    }
}
