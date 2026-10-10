package com.xperiencemusic.shared.presentation.library

import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.Song

fun Song.toLibraryItem(): LibrarySongUi = LibrarySongUi(
    id = id,
    title = title,
    artist = artist,
    keyLabel = key.label(Notation.SOLFEGE),
    recordingCount = recordings.size,
    maxRecordings = Song.MAX_RECORDINGS,
)
