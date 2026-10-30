package com.xperiencemusic.shared.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * Tells the screen reader which chip, segment or card is the chosen one, in French: the colour
 * alone says nothing to TalkBack or VoiceOver.
 */
fun Modifier.choiceState(selected: Boolean): Modifier = semantics {
    this.selected = selected
    stateDescription = choiceStateLabel(selected)
}

/** "sélectionné" or "non sélectionné", as read after the label. */
fun choiceStateLabel(selected: Boolean): String = if (selected) "sélectionné" else "non sélectionné"
