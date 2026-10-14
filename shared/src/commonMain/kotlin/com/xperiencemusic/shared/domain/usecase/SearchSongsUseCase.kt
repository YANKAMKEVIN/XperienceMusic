package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Songs sorted by title, filtered on title or artist, ignoring case and accents, and on [key] when
 * one is given.
 */
class SearchSongsUseCase(private val songs: SongRepository) {
    operator fun invoke(query: String, key: Key? = null): Flow<List<Song>> {
        val needle = query.trim().foldForSearch()
        return songs.observeSongs().map { all ->
            all.filter { song ->
                (key == null || song.key == key) && (
                    needle.isEmpty() ||
                        song.title.foldForSearch().contains(needle) ||
                        song.artist.orEmpty().foldForSearch().contains(needle)
                    )
            }.sortedBy { it.title.foldForSearch() }
        }
    }
}

private val accents = mapOf(
    'à' to 'a', 'â' to 'a', 'ä' to 'a', 'ç' to 'c', 'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
    'î' to 'i', 'ï' to 'i', 'ô' to 'o', 'ö' to 'o', 'ù' to 'u', 'û' to 'u', 'ü' to 'u', 'ÿ' to 'y',
)

internal fun String.foldForSearch(): String = lowercase().map { accents[it] ?: it }.joinToString("")
