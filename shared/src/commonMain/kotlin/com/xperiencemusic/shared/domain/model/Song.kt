package com.xperiencemusic.shared.domain.model

import com.xperiencemusic.music.theory.Key
import kotlinx.datetime.LocalDate

data class Song(
    val id: String,
    val title: String,
    val artist: String?,
    val key: Key,
    /** ChordPro-style chart with chords stored as Nashville numbers (see `ChordChart`). */
    val chart: String,
    val recordings: List<Recording>,
    /** Learning tracks per voice. They do not count towards [MAX_RECORDINGS]. */
    val voiceTracks: List<VoiceTrack>,
    val links: List<SongLink>,
) {
    val referenceRecording: Recording? get() = recordings.firstOrNull { it.isReference }

    companion object {
        /** One reference plus two alternatives, at most. */
        const val MAX_RECORDINGS = 3
    }
}

data class Recording(
    val id: String,
    val title: String,
    val recordedOn: LocalDate,
    val durationSeconds: Int,
    val audioUrl: String,
    val isReference: Boolean,
)

data class VoiceTrack(
    val id: String,
    val voicePart: VoicePart,
    val audioUrl: String,
)

data class SongLink(val label: String, val url: String)
