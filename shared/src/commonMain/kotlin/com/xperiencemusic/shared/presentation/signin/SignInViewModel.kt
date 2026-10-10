package com.xperiencemusic.shared.presentation.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.AppEnvironment
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.usecase.RequestSignInCodeUseCase
import com.xperiencemusic.shared.domain.usecase.VerifySignInCodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Once the code is accepted the session changes and the app root moves on by itself. */
class SignInViewModel(
    private val requestCode: RequestSignInCodeUseCase,
    private val verifyCode: VerifySignInCodeUseCase,
    environment: AppEnvironment,
) : ViewModel() {
    private val _state = MutableStateFlow(SignInUiState(demoCode = environment.demoCode))
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun onEvent(event: SignInEvent) {
        when (event) {
            is SignInEvent.EmailChanged -> _state.update { it.copy(email = event.email, error = null) }
            is SignInEvent.CodeChanged -> _state.update {
                it.copy(code = event.code.filter(Char::isDigit).take(SignInUiState.CODE_LENGTH), error = null)
            }
            SignInEvent.ChangeEmailClicked -> _state.update { it.copy(step = SignInStep.EMAIL, code = "", error = null) }
            SignInEvent.SubmitClicked -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (current.step) {
                SignInStep.EMAIL -> when (val result = requestCode(current.email)) {
                    is DomainResult.Success -> _state.update {
                        it.copy(isSubmitting = false, step = SignInStep.CODE, email = result.value)
                    }
                    is DomainResult.Failure -> fail(result.error)
                }
                SignInStep.CODE -> when (val result = verifyCode(current.email, current.code)) {
                    is DomainResult.Success -> _state.update { it.copy(isSubmitting = false) }
                    is DomainResult.Failure -> fail(result.error)
                }
            }
        }
    }

    private fun fail(error: DomainError) {
        val signInError = when (error) {
            DomainError.INVALID_INPUT -> SignInError.INVALID_EMAIL
            DomainError.INVALID_CODE -> SignInError.INVALID_CODE
            else -> SignInError.NETWORK
        }
        _state.update { it.copy(isSubmitting = false, error = signInError) }
    }
}
