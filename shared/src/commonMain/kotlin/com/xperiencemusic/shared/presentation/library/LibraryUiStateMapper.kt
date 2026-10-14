package com.xperiencemusic.shared.presentation.library

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.presentation.common.shortLabel

fun Song.toLibraryItem(): LibrarySongUi = LibrarySongUi(
    id = id,
    title = title,
    artist = artist,
    keyLabel = key.shortLabel(),
    recordingCount = recordings.size,
    maxRecordings = Song.MAX_RECORDINGS,
)

/** The distinct keys of [songs], majors first then minors, each from Do upwards. */
fun deriveKeyFilters(songs: List<Song>): List<Key> =
    songs.map { it.key }.distinct().sortedWith(compareBy({ it.mode }, { it.tonic }))
