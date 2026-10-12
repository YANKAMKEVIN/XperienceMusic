package com.xperiencemusic.shared.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.onboarding.OnboardingEvent
import com.xperiencemusic.shared.presentation.onboarding.OnboardingStep
import com.xperiencemusic.shared.presentation.onboarding.OnboardingUiState
import com.xperiencemusic.shared.presentation.onboarding.OnboardingViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.onboarding_back
import com.xperiencemusic.shared.resources.onboarding_continue
import com.xperiencemusic.shared.resources.onboarding_level_beginner
import com.xperiencemusic.shared.resources.onboarding_level_confirmed
import com.xperiencemusic.shared.resources.onboarding_level_pro
import com.xperiencemusic.shared.resources.onboarding_level_question
import com.xperiencemusic.shared.resources.onboarding_name_greeting
import com.xperiencemusic.shared.resources.onboarding_name_hint
import com.xperiencemusic.shared.resources.onboarding_name_label
import com.xperiencemusic.shared.resources.onboarding_name_question
import com.xperiencemusic.shared.resources.onboarding_role_bass
import com.xperiencemusic.shared.resources.onboarding_role_drums
import com.xperiencemusic.shared.resources.onboarding_role_piano
import com.xperiencemusic.shared.resources.onboarding_role_question
import com.xperiencemusic.shared.resources.onboarding_role_singer
import com.xperiencemusic.shared.resources.onboarding_sign_out
import com.xperiencemusic.shared.resources.onboarding_step
import com.xperiencemusic.shared.resources.onboarding_submit
import com.xperiencemusic.shared.resources.onboarding_subtitle
import com.xperiencemusic.shared.resources.onboarding_title
import com.xperiencemusic.shared.resources.onboarding_voice_alto
import com.xperiencemusic.shared.resources.onboarding_voice_bass
import com.xperiencemusic.shared.resources.onboarding_voice_help
import com.xperiencemusic.shared.resources.onboarding_voice_question
import com.xperiencemusic.shared.resources.onboarding_voice_soprano
import com.xperiencemusic.shared.resources.onboarding_voice_tenor
import com.xperiencemusic.shared.ui.brand.BrandBackground
import com.xperiencemusic.shared.ui.brand.BrandHeader
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.brand.XmMonogram
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(onSignOut: () -> Unit, viewModel: OnboardingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreenContent(state = state, onEvent = viewModel::onEvent, onSignOut = onSignOut)
}

@Composable
fun OnboardingScreenContent(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit, onSignOut: () -> Unit = {}) {
    val next = { onEvent(OnboardingEvent.ContinueClicked) }
    BrandBackground {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl),
            ) {
                if (state.step == OnboardingStep.NAME) {
                    BrandHeader()
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.s)) {
                        IconButton(onClick = { onEvent(OnboardingEvent.BackClicked) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.onboarding_back))
                        }
                        Spacer(Modifier.weight(1f))
                        XmMonogram(size = 40.dp)
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.size(48.dp))
                    }
                }
                StepProgress(state.stepIndex, state.steps.size, Modifier.padding(top = Spacing.l))
                Text(
                    stringResource(Res.string.onboarding_step, state.stepIndex + 1, state.steps.size),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.16.em),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.m),
                )
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
                    },
                    label = "step",
                ) { step ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m), modifier = Modifier.padding(top = Spacing.s, bottom = Spacing.l)) {
                        when (step) {
                            OnboardingStep.NAME -> {
                                NameStep(state, onEvent, next)
                                // Signed in with the wrong address: start again from the e-mail.
                                TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                    Text(stringResource(Res.string.onboarding_sign_out))
                                }
                            }
                            OnboardingStep.ROLE -> RoleStep(state, onEvent)
                            OnboardingStep.VOICE -> VoiceStep(state, onEvent)
                            OnboardingStep.LEVEL -> LevelStep(state, onEvent)
                        }
                    }
                }
            }
            GradientButton(
                onClick = next,
                enabled = state.canContinue,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.l),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        stringResource(if (state.isLastStep) Res.string.onboarding_submit else Res.string.onboarding_continue),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

/** One segment per question, filled with the brand gradient up to the current one. */
@Composable
private fun StepProgress(index: Int, count: Int, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth()) {
        repeat(count) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= index) BrandColors.horizontalGradient else SolidColor(MaterialTheme.colorScheme.outline)),
            )
        }
    }
}

@Composable
private fun Question(title: String, subtitle: String? = null) {
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun NameStep(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit, next: () -> Unit) {
    val onName: (String) -> Unit = { onEvent(OnboardingEvent.NameChanged(it)) }
    val label = stringResource(Res.string.onboarding_name_label)
    Text(stringResource(Res.string.onboarding_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    Text(stringResource(Res.string.onboarding_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Question(stringResource(Res.string.onboarding_name_question), stringResource(Res.string.onboarding_name_hint))
    NameFieldGreeting(
        state.name,
        onName,
        placeholder = label,
        greeting = stringResource(Res.string.onboarding_name_greeting, state.name.trim()),
        onDone = next,
        modifier = Modifier.padding(top = Spacing.l),
    )
}

@Composable
private fun RoleStep(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    Question(stringResource(Res.string.onboarding_role_question))
    listOf(
        Triple(Instrument.SINGER, XmIcons.Mic, Res.string.onboarding_role_singer),
        Triple(Instrument.PIANO, XmIcons.Piano, Res.string.onboarding_role_piano),
        Triple(Instrument.BASS, XmIcons.Bass, Res.string.onboarding_role_bass),
        Triple(Instrument.DRUMS, XmIcons.Drums, Res.string.onboarding_role_drums),
    ).forEach { (instrument, icon, description) ->
        val isSelected = state.instrument == instrument
        ChoiceCard(
            title = instrument.label(),
            description = stringResource(description),
            isSelected = isSelected,
            onClick = { onEvent(OnboardingEvent.InstrumentSelected(instrument)) },
        ) { IconTile(icon, isSelected) }
    }
}

@Composable
private fun VoiceStep(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    Question(stringResource(Res.string.onboarding_voice_question), stringResource(Res.string.onboarding_voice_help))
    val voices = listOf(
        VoicePart.SOPRANO to Res.string.onboarding_voice_soprano,
        VoicePart.ALTO to Res.string.onboarding_voice_alto,
        VoicePart.TENOR to Res.string.onboarding_voice_tenor,
        VoicePart.BASS to Res.string.onboarding_voice_bass,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), modifier = Modifier.fillMaxWidth().padding(top = Spacing.m)) {
        voices.forEachIndexed { index, (voice, _) ->
            VoiceColumn(
                label = voice.label(),
                heightFraction = 1f - index * 0.2f,
                isSelected = state.voicePart == voice,
                onClick = { onEvent(OnboardingEvent.VoicePartSelected(voice)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    val described = voices.firstOrNull { it.first == state.voicePart }
    Text(
        described?.let { stringResource(it.second) } ?: "",
        style = MaterialTheme.typography.titleMedium.copy(brush = BrandColors.horizontalGradient),
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
    )
}

@Composable
private fun LevelStep(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    Question(stringResource(Res.string.onboarding_level_question))
    listOf(
        Level.BEGINNER to Res.string.onboarding_level_beginner,
        Level.CONFIRMED to Res.string.onboarding_level_confirmed,
        Level.PRO to Res.string.onboarding_level_pro,
    ).forEachIndexed { index, (level, description) ->
        val isSelected = state.level == level
        ChoiceCard(
            title = level.label(),
            description = stringResource(description),
            isSelected = isSelected,
            onClick = { onEvent(OnboardingEvent.LevelSelected(level)) },
        ) { LevelBars(filled = index + 1, total = 3, isSelected = isSelected) }
    }
}

@Preview
@Composable
private fun OnboardingNamePreview() {
    AppTheme { OnboardingScreenContent(OnboardingUiState(name = "Sarah"), onEvent = {}) }
}

@Preview
@Composable
private fun OnboardingRolePreview() {
    AppTheme { OnboardingScreenContent(OnboardingUiState(step = OnboardingStep.ROLE, name = "Sarah", instrument = Instrument.SINGER), onEvent = {}) }
}

@Preview
@Composable
private fun OnboardingVoicePreview() {
    AppTheme {
        OnboardingScreenContent(
            OnboardingUiState(step = OnboardingStep.VOICE, name = "Sarah", instrument = Instrument.SINGER, voicePart = VoicePart.ALTO),
            onEvent = {},
        )
    }
}
