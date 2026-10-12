package com.xperiencemusic.shared.presentation.onboarding

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.VoicePart

/** One question per screen. The voice is only asked to singers. */
enum class OnboardingStep { NAME, ROLE, VOICE, LEVEL }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.NAME,
    val name: String = "",
    val instrument: Instrument? = null,
    val voicePart: VoicePart? = null,
    val level: Level? = null,
    val isSubmitting: Boolean = false,
) {
    val isVoicePartVisible: Boolean get() = instrument == Instrument.SINGER

    /** The questions this member goes through, in order. */
    val steps: List<OnboardingStep>
        // Until the role is chosen, assume a singer: the bar then shows 4 steps, the longest path.
        get() = OnboardingStep.entries.filter { it != OnboardingStep.VOICE || instrument == null || instrument == Instrument.SINGER }

    /** 0-based, for the progress bar. */
    val stepIndex: Int get() = steps.indexOf(step)

    val isLastStep: Boolean get() = step == steps.last()

    val canContinue: Boolean
        get() = !isSubmitting && when (step) {
            OnboardingStep.NAME -> name.isNotBlank()
            OnboardingStep.ROLE -> instrument != null
            OnboardingStep.VOICE -> voicePart != null
            OnboardingStep.LEVEL -> level != null
        }

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
    /** "Continuer"; on the last question it creates the profile. */
    data object ContinueClicked : OnboardingEvent
    data object BackClicked : OnboardingEvent
}
