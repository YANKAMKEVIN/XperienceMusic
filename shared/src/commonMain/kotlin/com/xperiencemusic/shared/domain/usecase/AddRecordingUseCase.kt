package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SongRepository

/**
 * Adds a recording to a song, keeping at most [Song.MAX_RECORDINGS].
 *
 * When the song is full, the caller must name the recording to [replace]; the new one takes its
 * place, and becomes the reference if the replaced one was. The first recording of a song is
 * always the reference.
 */
class AddRecordingUseCase(
    private val sessions: SessionRepository,
    private val songs: SongRepository,
) {
    suspend operator fun invoke(songId: String, recording: Recording, replace: String? = null): DomainResult<Song> {
        val song = when (val loaded = loadSongForRecordingChange(sessions, songs, songId)) {
            is DomainResult.Failure -> return loaded
            is DomainResult.Success -> loaded.value
        }

        recordingSlotError(song, replace)?.let { return DomainResult.Failure(it) }
        val replaced = song.recordings.firstOrNull { it.id == replace }
        val kept = song.recordings.filterNot { it.id == replaced?.id }

        val becomesReference = kept.isEmpty() || replaced?.isReference == true || recording.isReference
        val added = recording.copy(isReference = becomesReference)
        return runRemote {
            songs.addRecording(songId, added, replaced?.id)
            val updated = kept.map { if (becomesReference) it.copy(isReference = false) else it } + added
            song.copy(recordings = updated)
        }
    }
}

/** Why a recording cannot be added to [song] (replacing [replace] if set), or null when it can. */
internal fun recordingSlotError(song: Song, replace: String?): DomainError? {
    if (replace != null && song.recordings.none { it.id == replace }) return DomainError.NOT_FOUND
    val kept = song.recordings.count { it.id != replace }
    return if (kept >= Song.MAX_RECORDINGS) DomainError.RECORDING_LIMIT_REACHED else null
}
