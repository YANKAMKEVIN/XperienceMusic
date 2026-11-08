package com.xperiencemusic.shared.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.UpdateMyProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** "Mon profil": the welcome answers, open again; only the level stays with the leader. */
class ProfileViewModel(
    observeCurrentMember: ObserveCurrentMemberUseCase,
    private val updateMyProfile: UpdateMyProfileUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val member = observeCurrentMember().filterNotNull().first()
            _state.value = deriveProfileState(member.profile)
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.NameChanged -> _state.update { it.copy(name = event.name) }
            is ProfileEvent.InstrumentSelected -> _state.update { it.copy(instrument = event.instrument) }
            is ProfileEvent.VoicePartSelected -> _state.update { it.copy(voicePart = event.voicePart, isVoiceUnknown = false) }
            ProfileEvent.VoiceUnknownSelected -> _state.update { it.copy(voicePart = null, isVoiceUnknown = true) }
            ProfileEvent.SaveClicked -> save()
            ProfileEvent.ErrorShown -> _state.update { it.copy(hasError = false) }
        }
    }

    private fun save() {
        val current = _state.value
        val instrument = current.instrument ?: return
        if (!current.canSave) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = updateMyProfile(current.name, instrument, current.voicePart)
            _state.update { it.copy(isSaving = false, isDone = result is DomainResult.Success, hasError = result is DomainResult.Failure) }
        }
    }
}
