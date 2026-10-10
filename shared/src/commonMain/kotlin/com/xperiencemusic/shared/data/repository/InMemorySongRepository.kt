package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemorySongRepository(initialSongs: List<Song>) : SongRepository {
    private val songs = MutableStateFlow(initialSongs.associateBy { it.id })

    override fun observeSongs(): Flow<List<Song>> = songs.map { it.values.toList() }

    override fun observeSong(id: String): Flow<Song?> = songs.map { it[id] }

    override suspend fun song(id: String): Song? = songs.value[id]

    override suspend fun addRecording(songId: String, recording: Recording, replacingId: String?) =
        updateRecordings(songId) { recordings ->
            val kept = recordings.filterNot { it.id == replacingId }
            kept.map { if (recording.isReference) it.copy(isReference = false) else it } + recording
        }

    override suspend fun deleteRecording(songId: String, recordingId: String, newReferenceId: String?) =
        updateRecordings(songId) { recordings ->
            recordings.filterNot { it.id == recordingId }
                .map { if (newReferenceId != null) it.copy(isReference = it.id == newReferenceId) else it }
        }

    override suspend fun setReference(songId: String, recordingId: String) =
        updateRecordings(songId) { recordings -> recordings.map { it.copy(isReference = it.id == recordingId) } }

    private fun updateRecordings(songId: String, transform: (List<Recording>) -> List<Recording>) {
        songs.update { all ->
            val song = all[songId] ?: return@update all
            all + (songId to song.copy(recordings = transform(song.recordings)))
        }
    }
}
