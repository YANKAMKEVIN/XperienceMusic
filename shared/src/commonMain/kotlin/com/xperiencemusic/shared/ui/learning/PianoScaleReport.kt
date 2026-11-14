package com.xperiencemusic.shared.ui.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.xperiencemusic.music.pitch.PianoNoteCorrection
import com.xperiencemusic.music.pitch.PianoScaleComparison
import com.xperiencemusic.music.pitch.PianoScaleQuality
import com.xperiencemusic.music.pitch.PianoScaleReport
import com.xperiencemusic.music.pitch.PianoScaleStep
import com.xperiencemusic.shared.domain.learning.noteLabel
import com.xperiencemusic.shared.presentation.learning.PianoScaleUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.piano_scale_check_again
import com.xperiencemusic.shared.resources.piano_scale_compare
import com.xperiencemusic.shared.resources.piano_scale_compare_note
import com.xperiencemusic.shared.resources.piano_scale_detail_extra
import com.xperiencemusic.shared.resources.piano_scale_detail_match
import com.xperiencemusic.shared.resources.piano_scale_detail_missing
import com.xperiencemusic.shared.resources.piano_scale_detail_octave
import com.xperiencemusic.shared.resources.piano_scale_detail_wrong
import com.xperiencemusic.shared.resources.piano_scale_expected
import com.xperiencemusic.shared.resources.piano_scale_focus
import com.xperiencemusic.shared.resources.piano_scale_irregular
import com.xperiencemusic.shared.resources.piano_scale_marker
import com.xperiencemusic.shared.resources.piano_scale_matched
import com.xperiencemusic.shared.resources.piano_scale_no_verdict
import com.xperiencemusic.shared.resources.piano_scale_preparing_passage
import com.xperiencemusic.shared.resources.piano_scale_quality_clipped
import com.xperiencemusic.shared.resources.piano_scale_quality_quiet
import com.xperiencemusic.shared.resources.piano_scale_quality_short
import com.xperiencemusic.shared.resources.piano_scale_quality_uncertain
import com.xperiencemusic.shared.resources.piano_scale_regained
import com.xperiencemusic.shared.resources.piano_scale_regular
import com.xperiencemusic.shared.resources.piano_scale_replay_passage
import com.xperiencemusic.shared.resources.piano_scale_report_title
import com.xperiencemusic.shared.resources.piano_scale_same
import com.xperiencemusic.shared.resources.piano_scale_since_last
import com.xperiencemusic.shared.resources.piano_scale_status_check
import com.xperiencemusic.shared.resources.piano_scale_status_extra
import com.xperiencemusic.shared.resources.piano_scale_status_match
import com.xperiencemusic.shared.resources.piano_scale_status_missing
import com.xperiencemusic.shared.resources.piano_scale_status_octave
import com.xperiencemusic.shared.resources.piano_scale_tap_note
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** The take's report: note by note, the selected step in detail, and the comparison with the last take. */
@Composable
internal fun ScaleReport(state: PianoScaleUiState, report: PianoScaleReport, actions: PianoScaleActions) {
    AppCard {
        Text(
            stringResource(Res.string.piano_scale_report_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        if (report.quality != PianoScaleQuality.USABLE) {
            UnusableTake(report.quality)
        } else {
            Text(
                stringResource(Res.string.piano_scale_matched, report.matched, report.expectedNotes.size),
                Modifier.padding(top = Spacing.s).semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(stringResource(Res.string.piano_scale_tap_note), style = MaterialTheme.typography.bodySmall)
            StepButtons(state, report, actions)
            state.selectedStep?.let { index -> report.steps.getOrNull(index)?.let { StepDetail(state, index, it, actions) } }
            report.irregularSpacing?.let { irregular ->
                Text(
                    stringResource(if (irregular) Res.string.piano_scale_irregular else Res.string.piano_scale_regular),
                    Modifier.padding(top = Spacing.m),
                )
            }
            state.comparison?.let { Comparison(it, report) }
        }
    }
}

@Composable
private fun UnusableTake(quality: PianoScaleQuality) {
    Text(
        stringResource(
            when (quality) {
                PianoScaleQuality.TOO_SHORT -> Res.string.piano_scale_quality_short
                PianoScaleQuality.TOO_QUIET -> Res.string.piano_scale_quality_quiet
                PianoScaleQuality.CLIPPED -> Res.string.piano_scale_quality_clipped
                else -> Res.string.piano_scale_quality_uncertain
            },
        ),
        modifier = Modifier.padding(top = Spacing.s).semantics { liveRegion = LiveRegionMode.Polite },
    )
    Text(stringResource(Res.string.piano_scale_no_verdict), style = MaterialTheme.typography.bodySmall)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepButtons(state: PianoScaleUiState, report: PianoScaleReport, actions: PianoScaleActions) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        report.steps.forEachIndexed { index, step ->
            val status = stringResource(
                when (step.correction) {
                    PianoNoteCorrection.MATCH -> Res.string.piano_scale_status_match
                    PianoNoteCorrection.WRONG_OCTAVE -> Res.string.piano_scale_status_octave
                    PianoNoteCorrection.MISSING -> Res.string.piano_scale_status_missing
                    PianoNoteCorrection.EXTRA -> Res.string.piano_scale_status_extra
                    else -> Res.string.piano_scale_status_check
                },
            )
            val isSelected = state.selectedStep == index
            OutlinedButton(
                onClick = {
                    actions.select(index)
                    actions.passage(index)
                },
                enabled = !state.preparingReplay,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.heightIn(min = Sizes.tallTarget).semantics {
                    selected = isSelected
                    stateDescription = status
                },
            ) {
                Column {
                    Text(step.expected?.let(::noteLabel) ?: step.heard?.let { noteLabel(it.midi) }.orEmpty())
                    Text(status, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun StepDetail(state: PianoScaleUiState, index: Int, step: PianoScaleStep, actions: PianoScaleActions) {
    val expected = step.expected?.let(::noteLabel).orEmpty()
    val heard = step.heard?.let { noteLabel(it.midi) }.orEmpty()
    Text(
        when (step.correction) {
            PianoNoteCorrection.MATCH -> stringResource(Res.string.piano_scale_detail_match, expected)
            PianoNoteCorrection.WRONG_OCTAVE -> stringResource(Res.string.piano_scale_detail_octave, heard, expected)
            PianoNoteCorrection.WRONG_NOTE -> stringResource(Res.string.piano_scale_detail_wrong, heard, expected)
            PianoNoteCorrection.MISSING -> stringResource(Res.string.piano_scale_detail_missing, expected)
            PianoNoteCorrection.EXTRA -> stringResource(Res.string.piano_scale_detail_extra, heard)
        },
        Modifier.padding(top = Spacing.m).semantics { liveRegion = LiveRegionMode.Polite },
    )
    step.heard?.let {
        val marker = ((it.startSeconds * 10).toInt() / 10.0).toString()
        Text(stringResource(Res.string.piano_scale_marker, marker), style = MaterialTheme.typography.bodySmall)
    }
    TextButton(onClick = { actions.passage(index) }, enabled = !state.preparingReplay) {
        Text(stringResource(if (state.preparingReplay) Res.string.piano_scale_preparing_passage else Res.string.piano_scale_replay_passage))
    }
    if (step.expected != null) {
        TextButton(onClick = { actions.expected(index) }) { Text(stringResource(Res.string.piano_scale_expected, expected)) }
    }
    OutlinedButton(onClick = { actions.focus(index) }) { Text(stringResource(Res.string.piano_scale_focus)) }
}

@Composable
private fun Comparison(comparison: PianoScaleComparison, report: PianoScaleReport) {
    HorizontalDivider(Modifier.padding(vertical = Spacing.m))
    Text(stringResource(Res.string.piano_scale_since_last), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(Res.string.piano_scale_compare, comparison.previousMatched, comparison.currentMatched, report.expectedNotes.size))
    if (comparison.regained.isNotEmpty()) Text(stringResource(Res.string.piano_scale_regained, notesLabel(comparison.regained)))
    if (comparison.toCheckAgain.isNotEmpty()) Text(stringResource(Res.string.piano_scale_check_again, notesLabel(comparison.toCheckAgain)))
    if (comparison.regained.isEmpty() && comparison.toCheckAgain.isEmpty()) Text(stringResource(Res.string.piano_scale_same))
    Text(stringResource(Res.string.piano_scale_compare_note), style = MaterialTheme.typography.bodySmall)
}
