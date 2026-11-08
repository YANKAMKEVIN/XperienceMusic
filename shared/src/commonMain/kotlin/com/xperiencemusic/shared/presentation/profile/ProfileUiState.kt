package com.xperiencemusic.shared.presentation.profile

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.VoicePart

data class ProfileUiState(
    val isLoading: Boolean = true,
    /** What the server holds, to tell whether anything changed. */
    val saved: MemberProfile? = null,
    val name: String = "",
    val instrument: Instrument? = null,
    val voicePart: VoicePart? = null,
    /** A singer who does not know their part yet. */
    val isVoiceUnknown: Boolean = false,
    /** Shown, never edited here: the leader moves it on. */
    val level: Level? = null,
    val isSaving: Boolean = false,
    /** Saved: the screen closes. */
    val isDone: Boolean = false,
    val hasError: Boolean = false,
) {
    val isSinger: Boolean get() = instrument == Instrument.SINGER

    val hasChanges: Boolean
        get() = saved != null && (
            name.trim() != saved.displayName ||
                instrument != saved.instrument ||
                (isSinger && voicePart != saved.voicePart.takeIf { saved.instrument == Instrument.SINGER })
            )

    val canSave: Boolean get() = !isSaving && hasChanges && name.isNotBlank() && instrument != null
}

/** The form opened on the member's current answers. */
fun deriveProfileState(profile: MemberProfile): ProfileUiState = ProfileUiState(
    isLoading = false,
    saved = profile,
    name = profile.displayName,
    instrument = profile.instrument,
    voicePart = profile.voicePart,
    isVoiceUnknown = profile.instrument == Instrument.SINGER && profile.voicePart == null,
    level = profile.level,
)

sealed interface ProfileEvent {
    data class NameChanged(val name: String) : ProfileEvent
    data class InstrumentSelected(val instrument: Instrument) : ProfileEvent
    data class VoicePartSelected(val voicePart: VoicePart) : ProfileEvent
    data object VoiceUnknownSelected : ProfileEvent
    data object SaveClicked : ProfileEvent
    data object ErrorShown : ProfileEvent
}
