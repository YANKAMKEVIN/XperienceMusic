package com.xperiencemusic.shared.ui.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.domain.learning.noteLabel
import com.xperiencemusic.shared.domain.model.SingTarget
import com.xperiencemusic.shared.presentation.learning.LearningEvent
import com.xperiencemusic.shared.presentation.learning.LearningUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.learning_earphones
import com.xperiencemusic.shared.resources.learning_hearing
import com.xperiencemusic.shared.resources.learning_hears_nothing
import com.xperiencemusic.shared.resources.learning_listen_my_note
import com.xperiencemusic.shared.resources.learning_listen_my_notes
import com.xperiencemusic.shared.resources.learning_postpone_hint
import com.xperiencemusic.shared.resources.learning_sing_now
import com.xperiencemusic.shared.resources.learning_sing_step
import com.xperiencemusic.shared.resources.learning_sing_targets_many
import com.xperiencemusic.shared.resources.learning_sing_targets_one
import com.xperiencemusic.shared.resources.learning_stop_listening
import com.xperiencemusic.shared.resources.learning_sung
import com.xperiencemusic.shared.resources.learning_tuner_high
import com.xperiencemusic.shared.resources.learning_tuner_in_tune
import com.xperiencemusic.shared.resources.learning_tuner_low
import com.xperiencemusic.shared.resources.learning_tuner_none
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.MicrophoneDeniedNotice
import com.xperiencemusic.shared.ui.exercise.TunerNeedle
import com.xperiencemusic.shared.ui.platform.MicrophoneUse
import com.xperiencemusic.shared.ui.platform.rememberMicrophonePermission
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Sung activity: the notes to sing from the singer's comfort note, the microphone, and the live pitch. */
@Composable
internal fun SingPanel(state: LearningUiState, onEvent: (LearningEvent) -> Unit, hints: @Composable () -> Unit) {
    val target = state.activity.sing ?: return
    val permission = rememberMicrophonePermission(MicrophoneUse.LISTEN_VOICE)
    val notes = state.singTargets
    Text(
        stringResource(
            if (notes.size > 1) Res.string.learning_sing_targets_many else Res.string.learning_sing_targets_one,
            notesLabel(notes),
            secondsLabel(target.secondsEach),
        ),
        style = MaterialTheme.typography.titleMedium,
    )
    OutlinedButton(onClick = { onEvent(LearningEvent.ListenBroken) }, enabled = !state.singing && !state.saving) {
        Text(stringResource(if (notes.size > 1) Res.string.learning_listen_my_notes else Res.string.learning_listen_my_note))
    }
    if (target.accompanimentSemitones != null) {
        Text(stringResource(Res.string.learning_earphones), style = MaterialTheme.typography.bodySmall)
    }
    // High in the card: under the fixed button at the bottom, "Un indice" used to be out of sight.
    hints()
    when {
        state.singing -> SingingLive(state, target, onEvent)
        state.sung -> Text(stringResource(Res.string.learning_sung), color = MaterialTheme.colorScheme.primary)
        else -> {
            // After a refusal, "Ouvrir les réglages" comes first: below the button it fell under the fold on small phones.
            if (state.micDenied) state.micError?.let { MicrophoneDeniedNotice(it.text(), permission.openSettings) }
            GradientButton(
                onClick = { permission.request { if (it) onEvent(LearningEvent.StartSinging) else onEvent(LearningEvent.MicrophoneDenied) } },
                enabled = !state.saving && state.result != true,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            ) {
                Text(stringResource(Res.string.learning_sing_now), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    if (!state.micDenied) state.micError?.let { Text(it.text(), color = MaterialTheme.colorScheme.error) }
    if (state.canPostpone && !state.sung) {
        Text(stringResource(Res.string.learning_postpone_hint), style = MaterialTheme.typography.bodySmall)
    }
}

/** While the microphone listens: what it hears, how far from the note, and how far the take went. */
@Composable
private fun SingingLive(state: LearningUiState, target: SingTarget, onEvent: (LearningEvent) -> Unit) {
    val notes = state.singTargets
    OutlinedButton(onClick = { onEvent(LearningEvent.StopSinging) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.learning_stop_listening))
    }
    MicLevel(state.micLevel)
    Text(
        if (state.hearsNothing) {
            stringResource(Res.string.learning_hears_nothing)
        } else {
            stringResource(Res.string.learning_hearing, state.heardMidi?.let { noteLabel(it) } ?: "…")
        },
        style = MaterialTheme.typography.bodyLarge,
        color = if (state.hearsNothing) MaterialTheme.colorScheme.tertiary else Color.Unspecified,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    val cents = state.heardCents
    TunerNeedle(
        cents?.let { (it / 100).toFloat().coerceIn(-1f, 1f) },
        stringResource(
            when {
                cents == null -> Res.string.learning_tuner_none
                cents < -IN_TUNE_CENTS -> Res.string.learning_tuner_low
                cents > IN_TUNE_CENTS -> Res.string.learning_tuner_high
                else -> Res.string.learning_tuner_in_tune
            },
        ),
    )
    if (notes.size > 1) {
        Text(
            stringResource(Res.string.learning_sing_step, (state.singStep + 1).coerceAtMost(notes.size), notes.size),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    LinearProgressIndicator(
        progress = { (state.singStep + (state.holdSeconds / target.secondsEach).coerceIn(0.0, 1.0)).toFloat() / notes.size },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** What reaches the microphone, live: a bar that moves as soon as there is sound, even without a note. */
@Composable
private fun MicLevel(level: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.clearAndSetSemantics {},
    ) {
        Icon(XmIcons.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Box(
            Modifier
                .weight(1f)
                .height(Spacing.s)
                .clip(RoundedCornerShape(Spacing.xs))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(level.coerceIn(0f, 1f))
                    .height(Spacing.s)
                    .clip(RoundedCornerShape(Spacing.xs))
                    .background(BrandColors.horizontalGradient),
            )
        }
    }
}

private const val IN_TUNE_CENTS = 50
