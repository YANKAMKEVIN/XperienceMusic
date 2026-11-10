package com.xperiencemusic.shared.ui.signin

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.signin.SignInError
import com.xperiencemusic.shared.presentation.signin.SignInEvent
import com.xperiencemusic.shared.presentation.signin.SignInStep
import com.xperiencemusic.shared.presentation.signin.SignInUiState
import com.xperiencemusic.shared.presentation.signin.SignInViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.signin_change_email
import com.xperiencemusic.shared.resources.signin_checking
import com.xperiencemusic.shared.resources.signin_code_label
import com.xperiencemusic.shared.resources.signin_code_resent
import com.xperiencemusic.shared.resources.signin_code_subtitle
import com.xperiencemusic.shared.resources.signin_demo_hint
import com.xperiencemusic.shared.resources.signin_email_label
import com.xperiencemusic.shared.resources.signin_email_subtitle
import com.xperiencemusic.shared.resources.signin_error_code
import com.xperiencemusic.shared.resources.signin_error_email
import com.xperiencemusic.shared.resources.signin_error_empty
import com.xperiencemusic.shared.resources.signin_error_network
import com.xperiencemusic.shared.resources.signin_error_not_sent
import com.xperiencemusic.shared.resources.signin_error_too_many
import com.xperiencemusic.shared.resources.signin_resend
import com.xperiencemusic.shared.resources.signin_resend_in
import com.xperiencemusic.shared.resources.signin_send_code
import com.xperiencemusic.shared.resources.signin_title
import com.xperiencemusic.shared.ui.brand.BrandBackground
import com.xperiencemusic.shared.ui.brand.BrandHeader
import com.xperiencemusic.shared.ui.brand.BrandWordmark
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import com.xperiencemusic.shared.ui.theme.WordmarkSize
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInScreen(viewModel: SignInViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SignInScreenContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun SignInScreenContent(state: SignInUiState, onEvent: (SignInEvent) -> Unit) {
    val submit = { onEvent(SignInEvent.SubmitClicked) }
    // With the keyboard open, the full brand block would push the field off the screen.
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BrandBackground {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(top = Spacing.xl, start = Spacing.xl, end = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            if (keyboardOpen) {
                BrandWordmark(size = WordmarkSize.Small, showTagline = false, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                BrandHeader()
            }
            Text(stringResource(Res.string.signin_title), style = MaterialTheme.typography.headlineSmall)
            when (state.step) {
                SignInStep.EMAIL -> {
                    Text(
                        stringResource(Res.string.signin_email_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val isEmailError = state.error == SignInError.INVALID_EMAIL || state.error == SignInError.EMPTY_EMAIL
                    EmailFieldPill(
                        state.email,
                        onValueChange = { onEvent(SignInEvent.EmailChanged(it)) },
                        placeholder = stringResource(Res.string.signin_email_label),
                        sendLabel = stringResource(Res.string.signin_send_code),
                        isError = isEmailError,
                        canSend = state.canSubmit,
                        isSending = state.isSubmitting,
                        onSend = submit,
                    )
                    SignInErrorText(state.error)
                }

                SignInStep.CODE -> {
                    Text(
                        stringResource(Res.string.signin_code_subtitle, state.email),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CodeBoxes(
                        code = state.code,
                        length = SignInUiState.CODE_LENGTH,
                        onCodeChange = { onEvent(SignInEvent.CodeChanged(it)) },
                        isError = state.error == SignInError.INVALID_CODE,
                        enabled = !state.isSubmitting,
                        description = stringResource(Res.string.signin_code_label),
                    )
                    SignInErrorText(state.error)
                    if (state.codeResent) {
                        Text(
                            stringResource(Res.string.signin_code_resent),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    if (state.isSubmitting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                        ) {
                            CircularProgressIndicator(strokeWidth = Sizes.stroke, modifier = Modifier.size(Sizes.iconM))
                            Text(
                                stringResource(Res.string.signin_checking),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = Spacing.m),
                            )
                        }
                    }
                }
            }
            state.demoCode?.let {
                Text(
                    stringResource(Res.string.signin_demo_hint, it),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (state.step == SignInStep.CODE) {
                // Two lines of buttons rather than a label broken in two on a narrow phone.
                FlowRow(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = { onEvent(SignInEvent.ResendCodeClicked) }, enabled = state.canResend) {
                        Text(
                            if (state.resendInSeconds > 0) {
                                stringResource(Res.string.signin_resend_in, state.resendInSeconds)
                            } else {
                                stringResource(Res.string.signin_resend)
                            },
                        )
                    }
                    TextButton(onClick = { onEvent(SignInEvent.ChangeEmailClicked) }) {
                        Text(stringResource(Res.string.signin_change_email), softWrap = false, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Right under the field it is about, and scrolled into view: the keyboard must not hide it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SignInErrorText(error: SignInError?) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(error) { if (error != null) requester.bringIntoView() }
    error ?: return
    Text(
        stringResource(
            when (error) {
                SignInError.EMPTY_EMAIL -> Res.string.signin_error_empty
                SignInError.INVALID_EMAIL -> Res.string.signin_error_email
                SignInError.INVALID_CODE -> Res.string.signin_error_code
                SignInError.CODE_NOT_SENT -> Res.string.signin_error_not_sent
                SignInError.TOO_MANY_ATTEMPTS -> Res.string.signin_error_too_many
                SignInError.NETWORK -> Res.string.signin_error_network
            },
        ),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Preview
@Composable
private fun SignInScreenEmailPreview() {
    AppTheme { SignInScreenContent(SignInUiState(email = "grace@", error = SignInError.INVALID_EMAIL), onEvent = {}) }
}

@Preview
@Composable
private fun SignInScreenCodePreview() {
    AppTheme {
        SignInScreenContent(
            SignInUiState(step = SignInStep.CODE, email = "grace@exemple.com", code = "1234", demoCode = "123456"),
            onEvent = {},
        )
    }
}

