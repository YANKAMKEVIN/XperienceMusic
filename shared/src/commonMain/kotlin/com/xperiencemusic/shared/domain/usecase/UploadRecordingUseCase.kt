package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.AudioFile
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.repository.AudioStorage
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SongRepository
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Uploads an audio file and adds it as a recording of a song. Everything that can be checked is
 * checked before the upload, so a refused recording never costs a transfer; if adding fails after
 * the upload, the file is deleted again.
 */
class UploadRecordingUseCase(
    private val sessions: SessionRepository,
    private val songs: SongRepository,
    private val storage: AudioStorage,
    private val addRecording: AddRecordingUseCase,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        songId: String,
        title: String,
        recordedOn: LocalDate,
        audio: AudioFile,
        makeReference: Boolean,
        replace: String?,
    ): DomainResult<Song> {
        val song = when (val loaded = loadSongForRecordingChange(sessions, songs, songId)) {
            is DomainResult.Failure -> return loaded
            is DomainResult.Success -> loaded.value
        }
        recordingSlotError(song, replace)?.let { return DomainResult.Failure(it) }
        if (title.isBlank() || audio.bytes.isEmpty()) return DomainResult.Failure(DomainError.INVALID_INPUT)
        if (audio.bytes.size > AudioFile.MAX_BYTES) return DomainResult.Failure(DomainError.FILE_TOO_LARGE)

        val recordingId = Uuid.random().toString()
        val path = "songs/$songId/$recordingId" + audio.extension.let { if (it.isEmpty()) "" else ".$it" }
        when (val uploaded = runRemote { storage.upload(path, audio.mimeType, audio.bytes) }) {
            is DomainResult.Failure -> return uploaded
            is DomainResult.Success -> Unit
        }

        val recording = Recording(
            id = recordingId,
            title = title.trim(),
            recordedOn = recordedOn,
            // The database requires a positive duration; unknown durations are stored as 1 s.
            durationSeconds = (audio.durationSeconds ?: 0).coerceAtLeast(1),
            audioPath = path,
            isReference = makeReference,
        )
        val result = addRecording(songId, recording, replace)
        if (result is DomainResult.Failure) runRemote { storage.delete(path) }
        return result
    }
}
