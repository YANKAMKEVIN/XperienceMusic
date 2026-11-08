package com.xperiencemusic.shared.presentation.signin

data class SignInUiState(
    val step: SignInStep = SignInStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val isSubmitting: Boolean = false,
    val error: SignInError? = null,
    /** Shown in demo mode, where no email is sent. */
    val demoCode: String? = null,
    /** Seconds before another code can be asked for; 0 when it can. */
    val resendInSeconds: Int = 0,
    /** A new code has just gone out: said once, until the next digit is typed. */
    val codeResent: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && when (step) {
            SignInStep.EMAIL -> email.isNotBlank()
            SignInStep.CODE -> code.length == CODE_LENGTH
        }

    val canResend: Boolean
        get() = step == SignInStep.CODE && !isSubmitting && resendInSeconds == 0

    companion object {
        const val CODE_LENGTH = 6
        const val RESEND_DELAY_SECONDS = 30
    }
}

enum class SignInStep { EMAIL, CODE }

enum class SignInError { EMPTY_EMAIL, INVALID_EMAIL, INVALID_CODE, CODE_NOT_SENT, TOO_MANY_ATTEMPTS, NETWORK }

sealed interface SignInEvent {
    data class EmailChanged(val email: String) : SignInEvent
    data class CodeChanged(val code: String) : SignInEvent
    data object SubmitClicked : SignInEvent
    data object ChangeEmailClicked : SignInEvent
    data object ResendCodeClicked : SignInEvent
}
