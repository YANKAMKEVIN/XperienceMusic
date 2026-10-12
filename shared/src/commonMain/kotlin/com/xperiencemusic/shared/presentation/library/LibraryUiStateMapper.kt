package com.xperiencemusic.shared.presentation.library

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
