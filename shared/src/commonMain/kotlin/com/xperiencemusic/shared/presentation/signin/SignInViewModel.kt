package com.xperiencemusic.shared.presentation.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.AppEnvironment
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.usecase.RequestSignInCodeUseCase
import com.xperiencemusic.shared.domain.usecase.SignInLinkInbox
import com.xperiencemusic.shared.domain.usecase.VerifySignInCodeUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Once the code is accepted the session changes and the app root moves on by itself. */
class SignInViewModel(
    private val requestCode: RequestSignInCodeUseCase,
    private val verifyCode: VerifySignInCodeUseCase,
    environment: AppEnvironment,
    private val links: SignInLinkInbox,
) : ViewModel() {
    private val _state = MutableStateFlow(SignInUiState(demoCode = environment.demoCode))
    val state: StateFlow<SignInUiState> = _state.asStateFlow()
    private var resendCountdown: Job? = null

    init {
        // The e-mail's link: fill in the address and the code, then sign in straight away.
        viewModelScope.launch {
            links.link.filterNotNull().collect { link ->
                links.consume()
                if (_state.value.isSubmitting) return@collect
                _state.update {
                    it.copy(
                        step = SignInStep.CODE,
                        email = link.email,
                        code = link.code.take(SignInUiState.CODE_LENGTH),
                        error = null,
                    )
                }
                submit()
            }
        }
    }

    fun onEvent(event: SignInEvent) {
        when (event) {
            is SignInEvent.EmailChanged -> _state.update { it.copy(email = event.email, error = null) }
            is SignInEvent.CodeChanged -> {
                val wasComplete = _state.value.code.length == SignInUiState.CODE_LENGTH
                _state.update {
                    it.copy(
                        code = event.code.filter(Char::isDigit).take(SignInUiState.CODE_LENGTH),
                        error = null,
                        codeResent = false,
                    )
                }
                // The iPhone number pad has no "Done" key: the last digit signs in.
                if (!wasComplete && _state.value.code.length == SignInUiState.CODE_LENGTH) submit()
            }
            SignInEvent.ChangeEmailClicked -> _state.update {
                it.copy(step = SignInStep.EMAIL, code = "", error = null, codeResent = false)
            }
            SignInEvent.SubmitClicked -> submit()
            SignInEvent.ResendCodeClicked -> resend()
        }
    }

    private fun submit() {
        val current = _state.value
        // The arrow says why it does nothing rather than staying silent.
        if (current.step == SignInStep.EMAIL && current.email.isBlank() && !current.isSubmitting) {
            _state.update { it.copy(error = SignInError.EMPTY_EMAIL) }
            return
        }
        if (!current.canSubmit) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (current.step) {
                SignInStep.EMAIL -> when (val result = requestCode(current.email)) {
                    is DomainResult.Success -> {
                        _state.update { it.copy(isSubmitting = false, step = SignInStep.CODE, email = result.value) }
                        startResendCountdown()
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

    private fun resend() {
        val current = _state.value
        if (!current.canResend) return
        _state.update { it.copy(isSubmitting = true, error = null, codeResent = false) }
        viewModelScope.launch {
            when (val result = requestCode(current.email)) {
                is DomainResult.Success -> {
                    _state.update { it.copy(isSubmitting = false, code = "", codeResent = true) }
                    startResendCountdown()
                }
                is DomainResult.Failure -> fail(result.error)
            }
        }
    }

    /** A new code can be asked for after a short wait, so a slow e-mail is not answered by a flood of them. */
    private fun startResendCountdown() {
        resendCountdown?.cancel()
        resendCountdown = viewModelScope.launch {
            for (left in SignInUiState.RESEND_DELAY_SECONDS downTo 1) {
                _state.update { it.copy(resendInSeconds = left) }
                delay(1_000)
            }
            _state.update { it.copy(resendInSeconds = 0) }
        }
    }

    private fun fail(error: DomainError) {
        val signInError = when (error) {
            DomainError.INVALID_INPUT -> SignInError.INVALID_EMAIL
            DomainError.INVALID_CODE -> SignInError.INVALID_CODE
            DomainError.CODE_NOT_SENT -> SignInError.CODE_NOT_SENT
            DomainError.TOO_MANY_ATTEMPTS -> SignInError.TOO_MANY_ATTEMPTS
            else -> SignInError.NETWORK
        }
        // A refused code is cleared, so the next one is typed into empty boxes.
        _state.update {
            it.copy(
                isSubmitting = false,
                error = signInError,
                code = if (signInError == SignInError.INVALID_CODE) "" else it.code,
            )
        }
    }
}
