package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.BandMember
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.repository.MembersRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository

/** A super admin cannot change their own role: the band could end up with nobody to manage it. */
class ChangeMemberRoleUseCase(
    private val sessions: SessionRepository,
    private val members: MembersRepository,
) {
    suspend operator fun invoke(member: BandMember, role: Role): DomainResult<Unit> {
        val manager = when (val check = requireMemberManager(sessions)) {
            is DomainResult.Failure -> return check
            is DomainResult.Success -> check.value
        }
        if (member.id == manager.id) return DomainResult.Failure(DomainError.CANNOT_CHANGE_SELF)
        return runRemote { members.changeRole(member, role) }
    }
}
