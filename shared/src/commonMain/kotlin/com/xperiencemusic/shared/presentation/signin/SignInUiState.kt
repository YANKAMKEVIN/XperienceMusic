package com.xperiencemusic.shared.presentation.signin

data class SignInUiState(
    val step: SignInStep = SignInStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val isSubmitting: Boolean = false,
    val error: SignInError? = null,
    /** Shown in demo mode, where no email is sent. */
    val demoCode: String? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && when (step) {
            SignInStep.EMAIL -> email.isNotBlank()
            SignInStep.CODE -> code.length == CODE_LENGTH
        }

    companion object {
        const val CODE_LENGTH = 6
    }
}

enum class SignInStep { EMAIL, CODE }

enum class SignInError { INVALID_EMAIL, INVALID_CODE, CODE_NOT_SENT, TOO_MANY_ATTEMPTS, NETWORK }

sealed interface SignInEvent {
    data class EmailChanged(val email: String) : SignInEvent
    data class CodeChanged(val code: String) : SignInEvent
    data object SubmitClicked : SignInEvent
    data object ChangeEmailClicked : SignInEvent
}
