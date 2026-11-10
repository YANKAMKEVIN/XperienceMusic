package com.xperiencemusic.shared.domain.model

/** What a member shows in a circle: an avatar shipped with the app, or their own photo. */
sealed interface Avatar {
    data class Preset(val preset: AvatarPreset) : Avatar

    /** [path] is relative to the avatars bucket and starts with the member's id. */
    data class Photo(val path: String) : Avatar
}

/** The avatars drawn for the band, two per role for boys and two for girls. Anyone may pick any. */
enum class AvatarPreset(val instrument: Instrument) {
    SINGER_BOY_1(Instrument.SINGER),
    SINGER_BOY_2(Instrument.SINGER),
    SINGER_GIRL_1(Instrument.SINGER),
    SINGER_GIRL_2(Instrument.SINGER),
    PIANO_BOY_1(Instrument.PIANO),
    PIANO_BOY_2(Instrument.PIANO),
    PIANO_GIRL_1(Instrument.PIANO),
    PIANO_GIRL_2(Instrument.PIANO),
    BASS_BOY_1(Instrument.BASS),
    BASS_BOY_2(Instrument.BASS),
    BASS_GIRL_1(Instrument.BASS),
    BASS_GIRL_2(Instrument.BASS),
    DRUMS_BOY_1(Instrument.DRUMS),
    DRUMS_BOY_2(Instrument.DRUMS),
    DRUMS_GIRL_1(Instrument.DRUMS),
    DRUMS_GIRL_2(Instrument.DRUMS),
    ;

    companion object {
        /** The member's own role first, then the others in the usual order. */
        fun orderedFor(instrument: Instrument): List<AvatarPreset> =
            entries.sortedBy { if (it.instrument == instrument) 0 else 1 }
    }
}

/** The stored form, `preset:NAME` or `photo:path`, as kept in `profiles.avatar`. */
fun Avatar.encode(): String = when (this) {
    is Avatar.Preset -> "$PRESET_PREFIX${preset.name}"
    is Avatar.Photo -> "$PHOTO_PREFIX$path"
}

/** Null for anything unknown (a preset removed from a later version, say): initials are shown. */
fun decodeAvatar(stored: String?): Avatar? = when {
    stored == null -> null
    stored.startsWith(PRESET_PREFIX) ->
        AvatarPreset.entries.firstOrNull { it.name == stored.removePrefix(PRESET_PREFIX) }?.let(Avatar::Preset)
    stored.startsWith(PHOTO_PREFIX) -> stored.removePrefix(PHOTO_PREFIX).takeIf { it.isNotBlank() }?.let(Avatar::Photo)
    else -> null
}

private const val PRESET_PREFIX = "preset:"
private const val PHOTO_PREFIX = "photo:"
