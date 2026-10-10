package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SongRepository

/** Loads a song for a recording change, after checking the current member may manage recordings. */
internal suspend fun loadSongForRecordingChange(
    sessions: SessionRepository,
    songs: SongRepository,
    songId: String,
): DomainResult<Song> {
    val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
    if (!member.role.can(Permission.MANAGE_RECORDINGS)) return DomainResult.Failure(DomainError.FORBIDDEN)
    val song = songs.song(songId) ?: return DomainResult.Failure(DomainError.NOT_FOUND)
    return DomainResult.Success(song)
}
