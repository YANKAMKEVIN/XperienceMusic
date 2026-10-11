package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SongRepository

class DeleteSongUseCase(
    private val sessions: SessionRepository,
    private val songs: SongRepository,
) {
    suspend operator fun invoke(songId: String): DomainResult<Unit> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (!member.role.can(Permission.EDIT_SONG)) return DomainResult.Failure(DomainError.FORBIDDEN)
        return runRemote { songs.deleteSong(songId) }
    }
}
