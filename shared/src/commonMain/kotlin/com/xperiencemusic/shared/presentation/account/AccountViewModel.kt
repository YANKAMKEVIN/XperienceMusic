package com.xperiencemusic.shared.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.usecase.ChangeAvatarUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveReviewQueueUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    observeCurrentMember: ObserveCurrentMemberUseCase,
    observeReviewQueue: ObserveReviewQueueUseCase,
    private val changeAvatar: ChangeAvatarUseCase,
) : ViewModel() {
    private val action = MutableStateFlow(Action())

    /** What waits for the reviewer, as on the Progress tab; nothing for everyone else. */
    private val base = combine(observeCurrentMember(), observeReviewQueue()) { member, queue ->
        deriveAccountState(member).copy(toReviewCount = queue.toReview, toValidateCount = queue.toValidate)
    }

    val state: StateFlow<AccountUiState> = combine(base, action) { state, act ->
        state.copy(isSaving = act.isSaving, error = act.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    fun onEvent(event: AccountEvent) {
        when (event) {
            is AccountEvent.PresetChosen -> save { changeAvatar.choosePreset(event.preset) }
            is AccountEvent.PhotoPicked -> {
                val jpeg = event.jpeg
                if (jpeg == null) action.update { it.copy(error = AccountError.PHOTO_UNREADABLE) } else save { changeAvatar.usePhoto(jpeg) }
            }
            AccountEvent.RemoveAvatarClicked -> save { changeAvatar.remove() }
            AccountEvent.ErrorShown -> action.update { it.copy(error = null) }
        }
    }

    private fun save(block: suspend () -> DomainResult<Member>) {
        if (action.value.isSaving) return
        action.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = block()
            action.update { it.copy(isSaving = false, error = AccountError.SAVE_FAILED.takeIf { result is DomainResult.Failure }) }
        }
    }

    private data class Action(val isSaving: Boolean = false, val error: AccountError? = null)
}
