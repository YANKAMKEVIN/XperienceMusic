package com.xperiencemusic.shared.ui.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.signin.SignInError
import com.xperiencemusic.shared.presentation.signin.SignInEvent
import com.xperiencemusic.shared.presentation.signin.SignInStep
import com.xperiencemusic.shared.presentation.signin.SignInUiState
import com.xperiencemusic.shared.presentation.signin.SignInViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.signin_change_email
import com.xperiencemusic.shared.resources.signin_code_label
import com.xperiencemusic.shared.resources.signin_code_subtitle
import com.xperiencemusic.shared.resources.signin_demo_hint
import com.xperiencemusic.shared.resources.signin_email_label
import com.xperiencemusic.shared.resources.signin_email_subtitle
import com.xperiencemusic.shared.resources.signin_error_code
import com.xperiencemusic.shared.resources.signin_error_email
import com.xperiencemusic.shared.resources.signin_error_network
import com.xperiencemusic.shared.resources.signin_error_not_sent
import com.xperiencemusic.shared.resources.signin_error_too_many
import com.xperiencemusic.shared.resources.signin_send_code
import com.xperiencemusic.shared.resources.signin_title
import com.xperiencemusic.shared.resources.signin_verify
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
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
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Text(stringResource(Res.string.signin_title), style = MaterialTheme.typography.headlineMedium)
            when (state.step) {
                SignInStep.EMAIL -> {
                    Text(stringResource(Res.string.signin_email_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { onEvent(SignInEvent.EmailChanged(it)) },
                        label = { Text(stringResource(Res.string.signin_email_label)) },
                        singleLine = true,
                        isError = state.error == SignInError.INVALID_EMAIL,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SignInStep.CODE -> {
                    Text(
                        stringResource(Res.string.signin_code_subtitle, state.email),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = state.code,
                        onValueChange = { onEvent(SignInEvent.CodeChanged(it)) },
                        label = { Text(stringResource(Res.string.signin_code_label)) },
                        singleLine = true,
                        isError = state.error == SignInError.INVALID_CODE,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            state.demoCode?.let {
                Text(
                    stringResource(Res.string.signin_demo_hint, it),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            state.error?.let { error ->
                Text(
                    stringResource(
                        when (error) {
                            SignInError.INVALID_EMAIL -> Res.string.signin_error_email
                            SignInError.INVALID_CODE -> Res.string.signin_error_code
                            SignInError.CODE_NOT_SENT -> Res.string.signin_error_not_sent
                            SignInError.TOO_MANY_ATTEMPTS -> Res.string.signin_error_too_many
                            SignInError.NETWORK -> Res.string.signin_error_network
                        },
                    ),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Button(onClick = submit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth()) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(end = Spacing.s))
                }
                Text(
                    stringResource(if (state.step == SignInStep.EMAIL) Res.string.signin_send_code else Res.string.signin_verify),
                )
            }
            if (state.step == SignInStep.CODE) {
                TextButton(onClick = { onEvent(SignInEvent.ChangeEmailClicked) }) {
                    Text(stringResource(Res.string.signin_change_email))
                }
            }
        }
    }
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
