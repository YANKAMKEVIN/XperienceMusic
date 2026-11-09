package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.AudioFile
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.VoiceTrack
import com.xperiencemusic.shared.domain.repository.AudioStorage
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SongRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Uploads a song's track for one voice, in place of the previous one for that voice. Voice tracks
 * do not count towards the song's recordings. The old file only goes once the new one is in place.
 */
class UploadVoiceTrackUseCase(
    private val sessions: SessionRepository,
    private val songs: SongRepository,
    private val storage: AudioStorage,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(songId: String, part: VoicePart, audio: AudioFile): DomainResult<Unit> {
        val song = when (val loaded = loadSongForRecordingChange(sessions, songs, songId)) {
            is DomainResult.Failure -> return loaded
            is DomainResult.Success -> loaded.value
        }
        if (audio.bytes.isEmpty()) return DomainResult.Failure(DomainError.INVALID_INPUT)
        if (audio.bytes.size > AudioFile.MAX_BYTES) return DomainResult.Failure(DomainError.FILE_TOO_LARGE)

        val id = Uuid.random().toString()
        val path = "songs/$songId/voices/$id" + audio.extension.let { if (it.isEmpty()) "" else ".$it" }
        when (val uploaded = runRemote { storage.upload(path, audio.mimeType, audio.bytes) }) {
            is DomainResult.Failure -> return uploaded
            is DomainResult.Success -> Unit
        }
        val saved = runRemote { songs.setVoiceTrack(songId, VoiceTrack(id, part, path)) }
        if (saved is DomainResult.Failure) {
            runRemote { storage.delete(path) }
            return saved
        }
        // The track is in place either way; a leftover file only costs storage.
        song.voiceTracks.firstOrNull { it.voicePart == part }?.let { previous -> runRemote { storage.delete(previous.audioPath) } }
        return DomainResult.Success(Unit)
    }
}
