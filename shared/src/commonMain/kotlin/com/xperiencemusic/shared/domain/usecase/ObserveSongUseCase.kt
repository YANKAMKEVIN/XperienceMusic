package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class ObserveSongUseCase(private val songs: SongRepository) {
    operator fun invoke(id: String): Flow<Song?> = songs.observeSong(id)
}
