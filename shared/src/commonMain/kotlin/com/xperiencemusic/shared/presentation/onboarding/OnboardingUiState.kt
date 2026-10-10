package com.xperiencemusic.shared.presentation.onboarding

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.VoicePart

data class OnboardingUiState(
    val name: String = "",
    val instrument: Instrument? = null,
    val voicePart: VoicePart? = null,
    val level: Level? = null,
    val isSubmitting: Boolean = false,
) {
    val isVoicePartVisible: Boolean get() = instrument == Instrument.SINGER

    val canSubmit: Boolean
        get() = !isSubmitting &&
            name.isNotBlank() &&
            instrument != null &&
            level != null &&
            (!isVoicePartVisible || voicePart != null)
}

sealed interface OnboardingEvent {
    data class NameChanged(val name: String) : OnboardingEvent
    data class InstrumentSelected(val instrument: Instrument) : OnboardingEvent
    data class VoicePartSelected(val voicePart: VoicePart) : OnboardingEvent
    data class LevelSelected(val level: Level) : OnboardingEvent
    data object SubmitClicked : OnboardingEvent
}
