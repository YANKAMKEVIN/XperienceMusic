package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SetlistRepository

class PublishSetlistUseCase(
    private val sessions: SessionRepository,
    private val setlists: SetlistRepository,
) {
    suspend operator fun invoke(setlist: Setlist): DomainResult<Setlist> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (!member.role.can(Permission.PUBLISH_SETLIST)) return DomainResult.Failure(DomainError.FORBIDDEN)
        if (setlist.songIds.isEmpty() || setlist.serviceDate < setlist.rehearsalDate) {
            return DomainResult.Failure(DomainError.INVALID_INPUT)
        }
        val published = setlist.copy(publishedBy = member.profile.displayName)
        setlists.publish(published)
        return DomainResult.Success(published)
    }
}
