package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.chart.ChordChart
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.SongDraft
import com.xperiencemusic.shared.domain.model.YouTubeLink
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SongRepository

/**
 * Creates a song when [songId] is null, updates it otherwise. Returns the song id. The YouTube link
 * replaces the song's previous one; a blank link removes it.
 */
class SaveSongUseCase(
    private val sessions: SessionRepository,
    private val songs: SongRepository,
) {
    suspend operator fun invoke(songId: String?, draft: SongDraft): DomainResult<String> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (!member.role.can(Permission.EDIT_SONG)) return DomainResult.Failure(DomainError.FORBIDDEN)
        val title = draft.title.trim()
        if (title.isEmpty()) return DomainResult.Failure(DomainError.INVALID_INPUT)
        val youtubeUrl = draft.youtubeUrl?.takeIf { it.isNotBlank() }?.let {
            YouTubeLink.normalize(it) ?: return DomainResult.Failure(DomainError.INVALID_LINK)
        }

        val cleaned = draft.copy(
            title = title,
            artist = draft.artist?.trim()?.takeIf { it.isNotEmpty() },
            chart = ChordChart.normalize(draft.chart.trim(), draft.key),
            youtubeUrl = youtubeUrl,
        )
        return runRemote {
            if (songId == null) {
                songs.createSong(cleaned)
            } else {
                songs.updateSong(songId, cleaned)
                songId
            }
        }
    }
}
