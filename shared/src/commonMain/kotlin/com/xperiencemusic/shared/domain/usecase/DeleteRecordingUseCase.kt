package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SongRepository

/** Deletes a recording. If it was the reference, the most recent remaining one takes over. */
class DeleteRecordingUseCase(
    private val sessions: SessionRepository,
    private val songs: SongRepository,
) {
    suspend operator fun invoke(songId: String, recordingId: String): DomainResult<Song> {
        val song = when (val loaded = loadSongForRecordingChange(sessions, songs, songId)) {
            is DomainResult.Failure -> return loaded
            is DomainResult.Success -> loaded.value
        }
        val deleted = song.recordings.firstOrNull { it.id == recordingId }
            ?: return DomainResult.Failure(DomainError.NOT_FOUND)

        var remaining = song.recordings - deleted
        if (deleted.isReference) {
            val newReference = remaining.maxByOrNull { it.recordedOn }
            remaining = remaining.map { it.copy(isReference = it.id == newReference?.id) }
        }
        songs.saveRecordings(songId, remaining)
        return DomainResult.Success(song.copy(recordings = remaining))
    }
}
