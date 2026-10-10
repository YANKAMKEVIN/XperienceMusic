package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.chart.ChordChart
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.SongDraft
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SongRepository

/** Creates a song when [songId] is null, updates it otherwise. Returns the song id. */
class SaveSongUseCase(
    private val sessions: SessionRepository,
    private val songs: SongRepository,
) {
    suspend operator fun invoke(songId: String?, draft: SongDraft): DomainResult<String> {
        val member = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        if (!member.role.can(Permission.EDIT_SONG)) return DomainResult.Failure(DomainError.FORBIDDEN)
        val title = draft.title.trim()
        if (title.isEmpty()) return DomainResult.Failure(DomainError.INVALID_INPUT)

        val cleaned = draft.copy(
            title = title,
            artist = draft.artist?.trim()?.takeIf { it.isNotEmpty() },
            chart = ChordChart.normalize(draft.chart.trim(), draft.key),
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
