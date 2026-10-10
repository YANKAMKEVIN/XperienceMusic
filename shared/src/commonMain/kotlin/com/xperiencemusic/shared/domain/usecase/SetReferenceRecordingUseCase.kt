package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SongRepository

class SetReferenceRecordingUseCase(
    private val sessions: SessionRepository,
    private val songs: SongRepository,
) {
    suspend operator fun invoke(songId: String, recordingId: String): DomainResult<Song> {
        val song = when (val loaded = loadSongForRecordingChange(sessions, songs, songId)) {
            is DomainResult.Failure -> return loaded
            is DomainResult.Success -> loaded.value
        }
        if (song.recordings.none { it.id == recordingId }) return DomainResult.Failure(DomainError.NOT_FOUND)

        val updated = song.recordings.map { it.copy(isReference = it.id == recordingId) }
        songs.saveRecordings(songId, updated)
        return DomainResult.Success(song.copy(recordings = updated))
    }
}
