package com.xperiencemusic.shared.ui.learning

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.LearningKind
import com.xperiencemusic.shared.presentation.learning.CheckFeedback
import com.xperiencemusic.shared.presentation.learning.LearningEvent
import com.xperiencemusic.shared.presentation.learning.LearningUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.learning_check
import com.xperiencemusic.shared.resources.learning_continue
import com.xperiencemusic.shared.resources.learning_declarative_piano
import com.xperiencemusic.shared.resources.learning_declarative_singer
import com.xperiencemusic.shared.resources.learning_hint_all
import com.xperiencemusic.shared.resources.learning_hint_demo
import com.xperiencemusic.shared.resources.learning_hint_ear
import com.xperiencemusic.shared.resources.learning_hint_first
import com.xperiencemusic.shared.resources.learning_hint_keyboard
import com.xperiencemusic.shared.resources.learning_hint_keyboard_missing
import com.xperiencemusic.shared.resources.learning_hint_more
import com.xperiencemusic.shared.resources.learning_hint_other
import com.xperiencemusic.shared.resources.learning_hint_rhythm
import com.xperiencemusic.shared.resources.learning_hint_sing
import com.xperiencemusic.shared.resources.learning_keep_notes
import com.xperiencemusic.shared.resources.learning_keep_root
import com.xperiencemusic.shared.resources.learning_listen_broken
import com.xperiencemusic.shared.resources.learning_listen_chord
import com.xperiencemusic.shared.resources.learning_listen_first
import com.xperiencemusic.shared.resources.learning_listen_notes
import com.xperiencemusic.shared.resources.learning_listen_selection
import com.xperiencemusic.shared.resources.learning_metronome
import com.xperiencemusic.shared.resources.learning_postpone
import com.xperiencemusic.shared.resources.learning_practice_done
import com.xperiencemusic.shared.resources.learning_reset_taps
import com.xperiencemusic.shared.resources.learning_saving
import com.xperiencemusic.shared.resources.learning_selection
import com.xperiencemusic.shared.resources.learning_selection_none
import com.xperiencemusic.shared.resources.learning_tap
import com.xperiencemusic.shared.resources.learning_understood
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.theme.Radius
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** The fixed bottom of an activity: the verdict, then "Vérifier" or "Continuer". */
@Composable
internal fun ActivityAction(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    val activity = state.activity
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = Spacing.l, end = Spacing.l, top = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        // The answer to "Vérifier" sits right above it: never under the fold, and read out by TalkBack.
        state.checkFeedback?.let { feedback -> CheckFeedbackPanel(feedback, state.result == true, state.keptNotes) }
        // Before the singing, the real action is "Chanter maintenant" in the card: no grey button competing with it.
        val awaitsSinging = activity.kind == LearningKind.SING && !state.sung
        if (state.result == true) {
            GradientButton(onClick = { onEvent(LearningEvent.Next) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.learning_continue))
            }
        } else if (!awaitsSinging) {
            val label = when {
                state.saving -> Res.string.learning_saving
                activity.kind == LearningKind.LESSON -> Res.string.learning_understood
                activity.kind == LearningKind.PRACTICE -> Res.string.learning_practice_done
                else -> Res.string.learning_check
            }
            GradientButton(onClick = { onEvent(LearningEvent.Check) }, enabled = state.canCheck, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(label))
            }
        }
        // A sung activity never locks the session: no earphones, a noisy room or a shy microphone can wait.
        if (state.canPostpone && !state.sung) {
            TextButton(onClick = { onEvent(LearningEvent.Postpone) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.learning_postpone))
            }
        }
    }
}

@Composable
private fun CheckFeedbackPanel(feedback: CheckFeedback, passed: Boolean, kept: Set<Int>?) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 160.dp)
            .clip(RoundedCornerShape(Radius.card))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.m)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            feedback.text(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        kept?.let { notes ->
            val label = notesLabel(notes.sorted()).ifEmpty { stringResource(Res.string.learning_keep_root) }
            Text(stringResource(Res.string.learning_keep_notes, label), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Big answer buttons: two side by side ("Majeur" / "Mineur"), longer lists one per line. */
@Composable
private fun ChoiceButtons(choices: List<String>, selected: String?, enabled: Boolean, onPick: (String) -> Unit) {
    val button: @Composable (String, Modifier) -> Unit = { choice, modifier ->
        val isSelected = selected == choice
        OutlinedButton(
            onClick = { onPick(choice) },
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            ),
            border = BorderStroke(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ),
            modifier = modifier.heightIn(min = 56.dp).semantics { this.selected = isSelected },
        ) {
            Text(choice, textAlign = TextAlign.Center)
        }
    }
    if (choices.size <= 2) {
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            choices.forEach { button(it, Modifier.weight(1f)) }
        }
    } else {
        Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            choices.forEach { button(it, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
internal fun ActivityCard(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    val activity = state.activity
    val singer = state.module.instrument == Instrument.SINGER
    AppCard {
        Column(
            if (state.preferences.reduceMotion) Modifier else Modifier.animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(
                activity.kind.eyebrow(state.module.instrument),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(activity.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(activity.instruction, style = MaterialTheme.typography.bodyLarge)
            if (activity.kind in listOf(LearningKind.LESSON, LearningKind.PRACTICE)) Text(activity.explanation)
            if (activity.example.isNotEmpty()) ExampleButtons(state, onEvent)
            if (activity.kind in listOf(LearningKind.EAR, LearningKind.CHOICE)) {
                ChoiceButtons(state.choiceOptions, state.answer, enabled = !state.saving) { onEvent(LearningEvent.Answer(it)) }
                if (activity.kind == LearningKind.EAR && !state.listened) {
                    Text(stringResource(Res.string.learning_listen_first), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (activity.kind == LearningKind.KEYBOARD) KeyboardAnswer(state, onEvent)
            if (activity.kind == LearningKind.SING) SingPanel(state, onEvent) { HintBlock(state, onEvent) }
            if (activity.kind == LearningKind.RHYTHM) RhythmAnswer(state, onEvent)
            if (activity.kind != LearningKind.SING) HintBlock(state, onEvent)
            if (activity.kind == LearningKind.PRACTICE) {
                Text(
                    stringResource(if (singer) Res.string.learning_declarative_singer else Res.string.learning_declarative_piano),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ExampleButtons(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    val activity = state.activity
    OutlinedButton(onClick = { onEvent(LearningEvent.Listen) }, enabled = !state.saving) {
        val several = activity.example.size > 1 && activity.choices.isNotEmpty()
        Text(stringResource(if (several) Res.string.learning_listen_notes else Res.string.learning_listen_chord))
    }
    TextButton(onClick = { onEvent(LearningEvent.ListenBroken) }, enabled = !state.saving) {
        Text(stringResource(Res.string.learning_listen_broken))
    }
}

@Composable
private fun KeyboardAnswer(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    PianoKeyboard(state) { onEvent(LearningEvent.Key(it)) }
    val selection = notesLabel(state.notes.sorted()).ifEmpty { stringResource(Res.string.learning_selection_none) }
    Text(stringResource(Res.string.learning_selection, selection), style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = { onEvent(LearningEvent.ListenSelection) }, enabled = state.notes.isNotEmpty()) {
        Text(stringResource(Res.string.learning_listen_selection))
    }
}

@Composable
private fun RhythmAnswer(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    OutlinedButton(onClick = { onEvent(LearningEvent.Metronome) }) {
        Text(stringResource(Res.string.learning_metronome, state.activity.bpm))
    }
    Button(onClick = { onEvent(LearningEvent.Tap) }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)) {
        Text(stringResource(Res.string.learning_tap, state.taps.size))
    }
    TextButton(onClick = { onEvent(LearningEvent.ResetTaps) }) { Text(stringResource(Res.string.learning_reset_taps)) }
}

/** "Un indice", then more help on each tap; shown until the activity is passed. */
@Composable
internal fun HintBlock(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    val activity = state.activity
    if (activity.kind in listOf(LearningKind.LESSON, LearningKind.PRACTICE) || state.result == true) return
    TextButton(onClick = { onEvent(LearningEvent.Hint) }, enabled = state.hints < MAX_HINTS) {
        val label = when {
            state.hints == 0 -> Res.string.learning_hint_first
            state.hints < MAX_HINTS -> Res.string.learning_hint_more
            else -> Res.string.learning_hint_all
        }
        Text(stringResource(label))
    }
    if (state.hints > 0) {
        val hint = when {
            state.hints == 1 -> stringResource(
                when (activity.kind) {
                    LearningKind.KEYBOARD -> Res.string.learning_hint_keyboard
                    LearningKind.EAR -> Res.string.learning_hint_ear
                    LearningKind.RHYTHM -> Res.string.learning_hint_rhythm
                    LearningKind.SING -> Res.string.learning_hint_sing
                    else -> Res.string.learning_hint_other
                },
            )
            state.hints == 2 && activity.kind == LearningKind.KEYBOARD -> stringResource(Res.string.learning_hint_keyboard_missing)
            else -> activity.explanation
        }
        Text(hint, style = MaterialTheme.typography.bodyMedium)
    }
    if (state.hints >= MAX_HINTS && activity.expectedNotes.isNotEmpty()) {
        OutlinedButton(onClick = { onEvent(LearningEvent.ListenBroken) }) { Text(stringResource(Res.string.learning_hint_demo)) }
    }
}

private const val MAX_HINTS = 3
