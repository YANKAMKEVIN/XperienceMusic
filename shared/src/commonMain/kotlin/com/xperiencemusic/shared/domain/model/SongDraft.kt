package com.xperiencemusic.shared.domain.model

import com.xperiencemusic.music.theory.Key

/** What an admin edits about a song; recordings and voice tracks are managed separately. */
data class SongDraft(
    val title: String,
    val artist: String?,
    val key: Key,
    /** Chords may be typed in any notation; they are stored as Nashville numbers. */
    val chart: String,
)
