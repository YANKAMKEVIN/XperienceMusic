package com.xperiencemusic.shared.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.usecase.CompleteOnboardingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Once onboarding succeeds the session holds a member and the app root switches screens. */
class OnboardingViewModel(
    private val completeOnboarding: CompleteOnboardingUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.NameChanged -> _state.update { it.copy(name = event.name) }
            is OnboardingEvent.InstrumentSelected -> _state.update {
                // A musician has no voice part; keep none from an earlier choice.
                it.copy(instrument = event.instrument, voicePart = it.voicePart.takeIf { event.instrument == Instrument.SINGER })
            }
            is OnboardingEvent.VoicePartSelected -> _state.update { it.copy(voicePart = event.voicePart) }
            is OnboardingEvent.LevelSelected -> _state.update { it.copy(level = event.level) }
            OnboardingEvent.ContinueClicked -> next()
            OnboardingEvent.BackClicked -> _state.update { current ->
                current.copy(step = current.steps.getOrNull(current.stepIndex - 1) ?: current.step)
            }
        }
    }

    private fun next() {
        val current = _state.value
        if (!current.canContinue) return
        if (current.isLastStep) submit() else _state.update { it.copy(step = it.steps[it.stepIndex + 1]) }
    }

    private fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        val profile = MemberProfile(
            displayName = current.name.trim(),
            instrument = requireNotNull(current.instrument),
            voicePart = current.voicePart,
            level = requireNotNull(current.level),
        )
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            if (completeOnboarding(profile) is DomainResult.Failure) {
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
