package com.xperiencemusic.shared.domain.model

/** Long enough for any real name, short enough for the headers that show it. */
const val MAX_DISPLAY_NAME_LENGTH = 40

data class Member(
    val id: String,
    val profile: MemberProfile,
    val role: Role,
)

data class MemberProfile(
    val displayName: String,
    val instrument: Instrument,
    /** Only meaningful for singers. */
    val voicePart: VoicePart?,
    val level: Level,
)

enum class Instrument { SINGER, PIANO, BASS, DRUMS }

/** [comfortMidi] is a note any voice of that part reaches easily; exercises are built from it. */
enum class VoicePart(val comfortMidi: Int) {
    SOPRANO(67), // Sol4
    ALTO(62), // Ré4
    TENOR(55), // Sol3
    BASS(50), // Ré3
}

enum class Level { BEGINNER, CONFIRMED, PRO }
