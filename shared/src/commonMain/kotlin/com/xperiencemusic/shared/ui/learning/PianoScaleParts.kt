package com.xperiencemusic.shared.ui.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.xperiencemusic.music.pitch.PianoScaleKey
import com.xperiencemusic.shared.domain.learning.noteLabel
import com.xperiencemusic.shared.presentation.learning.PianoScaleMessage
import com.xperiencemusic.shared.presentation.learning.PianoScalePhase
import com.xperiencemusic.shared.presentation.learning.PianoScaleUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.piano_scale_ascending
import com.xperiencemusic.shared.resources.piano_scale_bpm
import com.xperiencemusic.shared.resources.piano_scale_collapsed
import com.xperiencemusic.shared.resources.piano_scale_current_beat
import com.xperiencemusic.shared.resources.piano_scale_descending
import com.xperiencemusic.shared.resources.piano_scale_expanded
import com.xperiencemusic.shared.resources.piano_scale_expected_note
import com.xperiencemusic.shared.resources.piano_scale_full_scale
import com.xperiencemusic.shared.resources.piano_scale_good_to_know_closed
import com.xperiencemusic.shared.resources.piano_scale_good_to_know_open
import com.xperiencemusic.shared.resources.piano_scale_key_c
import com.xperiencemusic.shared.resources.piano_scale_key_f
import com.xperiencemusic.shared.resources.piano_scale_key_g
import com.xperiencemusic.shared.resources.piano_scale_msg_analysis
import com.xperiencemusic.shared.resources.piano_scale_msg_denied
import com.xperiencemusic.shared.resources.piano_scale_msg_failed
import com.xperiencemusic.shared.resources.piano_scale_msg_interrupted
import com.xperiencemusic.shared.resources.piano_scale_msg_passage
import com.xperiencemusic.shared.resources.piano_scale_msg_stopped
import com.xperiencemusic.shared.resources.piano_scale_msg_timeout
import com.xperiencemusic.shared.resources.piano_scale_note_change
import com.xperiencemusic.shared.resources.piano_scale_note_experimental
import com.xperiencemusic.shared.resources.piano_scale_note_local
import com.xperiencemusic.shared.resources.piano_scale_note_tempo
import com.xperiencemusic.shared.resources.piano_scale_settings_title
import com.xperiencemusic.shared.resources.piano_scale_target
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.choiceState
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PianoScaleKey.label(): String = stringResource(
    when (this) {
        PianoScaleKey.C -> Res.string.piano_scale_key_c
        PianoScaleKey.G -> Res.string.piano_scale_key_g
        PianoScaleKey.F -> Res.string.piano_scale_key_f
    },
)

@Composable
internal fun directionLabel(descending: Boolean): String =
    stringResource(if (descending) Res.string.piano_scale_descending else Res.string.piano_scale_ascending)

@Composable
internal fun PianoScaleMessage.text(): String = stringResource(
    when (this) {
        PianoScaleMessage.MICROPHONE_DENIED -> Res.string.piano_scale_msg_denied
        PianoScaleMessage.TEST_TIMED_OUT -> Res.string.piano_scale_msg_timeout
        PianoScaleMessage.MICROPHONE_STOPPED -> Res.string.piano_scale_msg_stopped
        PianoScaleMessage.MICROPHONE_FAILED -> Res.string.piano_scale_msg_failed
        PianoScaleMessage.ANALYSIS_FAILED -> Res.string.piano_scale_msg_analysis
        PianoScaleMessage.PASSAGE_FAILED -> Res.string.piano_scale_msg_passage
        PianoScaleMessage.INTERRUPTED -> Res.string.piano_scale_msg_interrupted
    },
)

private val TEMPOS = listOf(40, 60, 80)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PracticeSettings(state: PianoScaleUiState, actions: PianoScaleActions) {
    val practice = state.practice
    AppCard {
        Text(stringResource(Res.string.piano_scale_settings_title), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            PianoScaleKey.entries.forEach { key ->
                FilterChip(
                    selected = practice.key == key,
                    onClick = { actions.configure(practice.copy(key = key, focusStart = null)) },
                    label = { Text(key.label()) },
                    modifier = Modifier.choiceState(practice.key == key),
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            listOf(false, true).forEach { down ->
                FilterChip(
                    selected = practice.descending == down,
                    onClick = { actions.configure(practice.copy(descending = down, focusStart = null)) },
                    label = { Text(directionLabel(down)) },
                    modifier = Modifier.choiceState(practice.descending == down),
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            TEMPOS.forEach { bpm ->
                FilterChip(
                    selected = practice.bpm == bpm,
                    onClick = { actions.configure(practice.copy(bpm = bpm)) },
                    label = { Text(stringResource(Res.string.piano_scale_bpm, bpm)) },
                    modifier = Modifier.choiceState(practice.bpm == bpm),
                )
            }
        }
        if (practice.focusStart != null) {
            TextButton(onClick = { actions.configure(practice.copy(focusStart = null)) }) {
                Text(stringResource(Res.string.piano_scale_full_scale))
            }
        }
    }
}

/** The small print of the workshop, folded: said once instead of under every block. */
@Composable
internal fun GoodToKnow() {
    var open by remember { mutableStateOf(false) }
    val notes = listOf(
        Res.string.piano_scale_note_experimental,
        Res.string.piano_scale_note_local,
        Res.string.piano_scale_note_tempo,
        Res.string.piano_scale_note_change,
    )
    val folding = stringResource(if (open) Res.string.piano_scale_expanded else Res.string.piano_scale_collapsed)
    AppCard {
        TextButton(
            onClick = { open = !open },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { stateDescription = folding },
        ) {
            Text(
                stringResource(if (open) Res.string.piano_scale_good_to_know_open else Res.string.piano_scale_good_to_know_closed),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (open) {
            notes.forEach { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = Spacing.s)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TargetNotes(state: PianoScaleUiState) {
    val current = stringResource(Res.string.piano_scale_current_beat)
    val expected = stringResource(Res.string.piano_scale_expected_note)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        state.practice.notes.forEachIndexed { index, note ->
            val active = state.phase == PianoScalePhase.RECORDING && state.visualBeat == index
            Surface(
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.semantics { stateDescription = if (active) current else expected },
            ) {
                Text(
                    stringResource(Res.string.piano_scale_target, index + 1, noteLabel(note)),
                    Modifier.padding(Spacing.m),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
