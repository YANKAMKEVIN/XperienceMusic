package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.AudioFile
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Song
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
        audioError(audio)?.let { return DomainResult.Failure(it) }

        val id = Uuid.random().toString()
        val path = "songs/$songId/voices/$id" + audio.extension.let { if (it.isEmpty()) "" else ".$it" }
        return replaceTrack(song, VoiceTrack(id, part, path), audio)
    }

    private fun audioError(audio: AudioFile): DomainError? = when {
        audio.bytes.isEmpty() -> DomainError.INVALID_INPUT
        audio.bytes.size > AudioFile.MAX_BYTES -> DomainError.FILE_TOO_LARGE
        else -> null
    }

    private suspend fun replaceTrack(song: Song, track: VoiceTrack, audio: AudioFile): DomainResult<Unit> {
        val uploaded = runRemote { storage.upload(track.audioPath, audio.mimeType, audio.bytes) }
        if (uploaded is DomainResult.Failure) return uploaded
        val saved = runRemote { songs.setVoiceTrack(song.id, track) }
        if (saved is DomainResult.Failure) {
            runRemote { storage.delete(track.audioPath) }
            return saved
        }
        // The track is in place either way; a leftover file only costs storage.
        song.voiceTracks.firstOrNull { it.voicePart == track.voicePart }
            ?.let { previous -> runRemote { storage.delete(previous.audioPath) } }
        return DomainResult.Success(Unit)
    }
}
