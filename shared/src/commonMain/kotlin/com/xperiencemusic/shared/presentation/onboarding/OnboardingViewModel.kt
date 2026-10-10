package com.xperiencemusic.shared.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainResult
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
            is OnboardingEvent.InstrumentSelected -> _state.update { it.copy(instrument = event.instrument) }
            is OnboardingEvent.VoicePartSelected -> _state.update { it.copy(voicePart = event.voicePart) }
            is OnboardingEvent.LevelSelected -> _state.update { it.copy(level = event.level) }
            OnboardingEvent.SubmitClicked -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        val profile = MemberProfile(
            displayName = current.name,
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
