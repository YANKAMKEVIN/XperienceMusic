package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.repository.SessionRepository

/** The signed-in member when they may manage members, the failure to return otherwise. */
internal suspend fun requireMemberManager(sessions: SessionRepository): DomainResult<Member> {
    val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
    if (!member.role.can(Permission.MANAGE_MEMBERS)) return DomainResult.Failure(DomainError.FORBIDDEN)
    return DomainResult.Success(member)
}
