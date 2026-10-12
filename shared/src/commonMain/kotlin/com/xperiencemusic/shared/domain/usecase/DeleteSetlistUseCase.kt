package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import kotlinx.datetime.LocalDate

/** Removes a week's setlist: a service cancelled, a list sent by mistake. */
class DeleteSetlistUseCase(
    private val sessions: SessionRepository,
    private val setlists: SetlistRepository,
) {
    suspend operator fun invoke(serviceDate: LocalDate): DomainResult<Unit> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (!member.role.can(Permission.PUBLISH_SETLIST)) return DomainResult.Failure(DomainError.FORBIDDEN)
        return runRemote { setlists.delete(serviceDate) }
    }
}
