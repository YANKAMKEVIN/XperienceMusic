package com.xperiencemusic.shared.domain.model

import com.xperiencemusic.music.theory.Key
import kotlinx.datetime.LocalDate

data class Song(
    val id: String,
    val title: String,
    val artist: String?,
    val key: Key,
    /**
     * ChordPro-style chart with chords stored as Nashville numbers (see `ChordChart`). Null while
     * it is not on the phone yet: the list of songs is loaded without the charts, and a song's
     * chart arrives when its page opens (or when a search reads the lyrics).
     */
    val chart: String?,
    val recordings: List<Recording>,
    /** Learning tracks per voice. They do not count towards [MAX_RECORDINGS]. */
    val voiceTracks: List<VoiceTrack>,
    val links: List<SongLink>,
    /** Its themes (adoration, louange…), imported from Rephi's tags; empty until they are. */
    val themes: List<String> = emptyList(),
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
    /** Location of the audio file in storage (a bucket path with Supabase). */
    val audioPath: String,
    val isReference: Boolean,
)

data class VoiceTrack(
    val id: String,
    val voicePart: VoicePart,
    val audioPath: String,
)

data class SongLink(val label: String, val url: String)
