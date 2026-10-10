package com.xperiencemusic.shared.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.onboarding.OnboardingEvent
import com.xperiencemusic.shared.presentation.onboarding.OnboardingUiState
import com.xperiencemusic.shared.presentation.onboarding.OnboardingViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.onboarding_instrument_title
import com.xperiencemusic.shared.resources.onboarding_level_title
import com.xperiencemusic.shared.resources.onboarding_name_label
import com.xperiencemusic.shared.resources.onboarding_submit
import com.xperiencemusic.shared.resources.onboarding_subtitle
import com.xperiencemusic.shared.resources.onboarding_title
import com.xperiencemusic.shared.resources.onboarding_voice_title
import com.xperiencemusic.shared.ui.components.ChoiceChips
import com.xperiencemusic.shared.ui.components.SectionTitle
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreenContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun OnboardingScreenContent(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Text(stringResource(Res.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(Res.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = { onEvent(OnboardingEvent.NameChanged(it)) },
                label = { Text(stringResource(Res.string.onboarding_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(stringResource(Res.string.onboarding_instrument_title))
            ChoiceChips(
                options = Instrument.entries,
                selected = state.instrument,
                label = { it.label() },
                onSelect = { onEvent(OnboardingEvent.InstrumentSelected(it)) },
            )

            if (state.isVoicePartVisible) {
                SectionTitle(stringResource(Res.string.onboarding_voice_title))
                ChoiceChips(
                    options = VoicePart.entries,
                    selected = state.voicePart,
                    label = { it.label() },
                    onSelect = { onEvent(OnboardingEvent.VoicePartSelected(it)) },
                )
            }

            SectionTitle(stringResource(Res.string.onboarding_level_title))
            ChoiceChips(
                options = Level.entries,
                selected = state.level,
                label = { it.label() },
                onSelect = { onEvent(OnboardingEvent.LevelSelected(it)) },
            )

            Button(
                onClick = { onEvent(OnboardingEvent.SubmitClicked) },
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.l),
            ) {
                Text(stringResource(Res.string.onboarding_submit))
            }
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenEmptyPreview() {
    AppTheme { OnboardingScreenContent(OnboardingUiState(), onEvent = {}) }
}

@Preview
@Composable
private fun OnboardingScreenSingerPreview() {
    AppTheme {
        OnboardingScreenContent(
            OnboardingUiState(name = "Grâce", instrument = Instrument.SINGER, voicePart = VoicePart.ALTO, level = Level.CONFIRMED),
            onEvent = {},
        )
    }
}
