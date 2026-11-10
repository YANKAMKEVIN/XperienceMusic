package com.xperiencemusic.shared.ui.learning

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.domain.learning.noteLabel
import com.xperiencemusic.shared.presentation.learning.LearningUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.learning_key_missing
import com.xperiencemusic.shared.resources.learning_key_selected
import com.xperiencemusic.shared.resources.learning_key_unselected
import com.xperiencemusic.shared.resources.learning_key_wrong
import com.xperiencemusic.shared.resources.learning_keyboard_scroll
import com.xperiencemusic.shared.ui.theme.Palette
import org.jetbrains.compose.resources.stringResource

private val WHITE_KEY_WIDTH = 72.dp
private val WHITE_KEY_HEIGHT = 180.dp
private val BLACK_KEY_WIDTH = 48.dp
private val BLACK_KEY_HEIGHT = 108.dp
private val KEYBOARD_HEIGHT = 184.dp
private val KEY_CORNER = 6.dp
private val KEY_LABEL_PADDING = 10.dp
private const val FIRST_KEY = 60
private const val LAST_KEY = 84

/** Two octaves from Do4, scrolled to the notes of the activity. */
@Composable
internal fun PianoKeyboard(state: LearningUiState, onKey: (Int) -> Unit) {
    val whites = (FIRST_KEY..LAST_KEY).filter { it % 12 in listOf(0, 2, 4, 5, 7, 9, 11) }
    val blacks = (FIRST_KEY + 1 until LAST_KEY).filter { it % 12 in listOf(1, 3, 6, 8, 10) }
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state.activity.id, state.encounter) {
        val first = state.activity.expectedNotes.minOrNull() ?: FIRST_KEY
        scroll.scrollTo(with(density) { (WHITE_KEY_WIDTH * whites.count { it < first }.coerceAtLeast(0)).roundToPx() })
    }
    fun press(midi: Int) {
        if (state.preferences.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onKey(midi)
    }
    Text(stringResource(Res.string.learning_keyboard_scroll), style = MaterialTheme.typography.bodySmall)
    Box(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
        Box(Modifier.width(WHITE_KEY_WIDTH * whites.size).height(KEYBOARD_HEIGHT)) {
            Row {
                whites.forEach { midi ->
                    PianoKey(midi, state, Modifier.width(WHITE_KEY_WIDTH).height(WHITE_KEY_HEIGHT), black = false) { press(midi) }
                }
            }
            blacks.forEach { midi ->
                val boundary = whites.count { it < midi }
                val offset = WHITE_KEY_WIDTH * boundary - BLACK_KEY_WIDTH / 2
                PianoKey(midi, state, Modifier.offset(x = offset).width(BLACK_KEY_WIDTH).height(BLACK_KEY_HEIGHT), black = true) {
                    press(midi)
                }
            }
        }
    }
}

@Composable
private fun PianoKey(midi: Int, state: LearningUiState, modifier: Modifier, black: Boolean, onClick: () -> Unit) {
    val selected = midi in state.notes
    val wrong = state.result == false && selected && midi !in state.activity.expectedNotes
    val missing = state.hints >= 2 && midi in state.activity.expectedNotes && !selected
    val color = when {
        wrong -> MaterialTheme.colorScheme.error
        state.result == true && selected -> Palette.Success
        selected -> MaterialTheme.colorScheme.primary
        missing -> MaterialTheme.colorScheme.tertiary
        black -> Palette.Ebony
        else -> Palette.Snow
    }
    val label = noteLabel(midi)
    val keyColor by animateColorAsState(color, tween(if (state.preferences.reduceMotion) 0 else 140), label = "key-color")
    val shape = RoundedCornerShape(bottomStart = KEY_CORNER, bottomEnd = KEY_CORNER)
    val description = stringResource(
        when {
            wrong -> Res.string.learning_key_wrong
            selected -> Res.string.learning_key_selected
            missing -> Res.string.learning_key_missing
            else -> Res.string.learning_key_unselected
        },
    )
    Box(
        modifier
            .background(keyColor, shape)
            .border(1.dp, Palette.KeyEdge, shape)
            .toggleable(selected, enabled = !state.saving, role = Role.Checkbox, onValueChange = { onClick() })
            .semantics {
                contentDescription = label
                stateDescription = description
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            label,
            color = if (black && !selected && !missing) Color.White else Palette.Ebony,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = KEY_LABEL_PADDING),
        )
    }
}
