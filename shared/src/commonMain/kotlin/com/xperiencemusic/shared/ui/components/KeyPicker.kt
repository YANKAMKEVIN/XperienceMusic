package com.xperiencemusic.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Mode
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.music.theory.SEMITONES_PER_OCTAVE
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.editor_key_chosen
import com.xperiencemusic.shared.resources.editor_major
import com.xperiencemusic.shared.resources.editor_minor
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * The twelve tonics as chips, always spelled the major-key way so the list does not change under
 * the finger when the mode flips (an imported C# minor used to turn Réb into Do#), a major/minor
 * switch, and the chosen key in full underneath.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyPicker(
    key: Key,
    onTonicSelected: (Int) -> Unit,
    onModeSelected: (Mode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            for (tonic in 0 until SEMITONES_PER_OCTAVE) {
                FilterChip(
                    selected = key.tonic == tonic,
                    onClick = { onTonicSelected(tonic) },
                    label = { Text(Key(tonic, Mode.MAJOR).tonicName(Notation.SOLFEGE)) },
                    modifier = Modifier.choiceState(key.tonic == tonic),
                )
            }
        }
        Text(
            stringResource(Res.string.editor_key_chosen, key.label(Notation.SOLFEGE)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val modes = listOf(Mode.MAJOR to Res.string.editor_major, Mode.MINOR to Res.string.editor_minor)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = key.mode == mode,
                    onClick = { onModeSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    label = { Text(stringResource(label)) },
                )
            }
        }
    }
}
