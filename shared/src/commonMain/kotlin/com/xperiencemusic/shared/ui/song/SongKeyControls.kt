package com.xperiencemusic.shared.ui.song

import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.xperiencemusic.shared.resources.cd_key
import com.xperiencemusic.shared.resources.cd_transpose_down
import com.xperiencemusic.shared.resources.cd_transpose_up
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Radius
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.presentation.song.SongDetailEvent
import com.xperiencemusic.shared.presentation.song.SongDetailUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.notation_letters
import com.xperiencemusic.shared.resources.notation_nashville
import com.xperiencemusic.shared.resources.notation_solfege
import com.xperiencemusic.shared.resources.song_original_key
import com.xperiencemusic.shared.resources.song_transpose_reset
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

/** The key between − and + keeps its width as it changes ("Do" to "Fa#m"). */
private val KEY_LABEL_MIN_WIDTH = 96.dp

/** The transposer pill (− key +) over the notation switch, as in the brand mockups. */
@Composable
internal fun KeyAndNotation(state: SongDetailUiState, onEvent: (SongDetailEvent) -> Unit) {
    val transposeDown = stringResource(Res.string.cd_transpose_down)
    val transposeUp = stringResource(Res.string.cd_transpose_up)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(Radius.card),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(Sizes.hairline, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onEvent(SongDetailEvent.TransposeDownClicked) },
                    modifier = Modifier.semantics { contentDescription = transposeDown },
                ) {
                    Text("−", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics {})
                }
                // The key rolls up when it goes higher and down when it goes lower.
                AnimatedContent(
                    targetState = state.transposeSemitones to state.keyLabel,
                    transitionSpec = {
                        val direction = if (targetState.first < initialState.first) -1 else 1
                        (slideInVertically { it * direction } + fadeIn())
                            .togetherWith(slideOutVertically { -it * direction } + fadeOut())
                    },
                    contentAlignment = Alignment.Center,
                    label = "key",
                    modifier = Modifier.widthIn(min = KEY_LABEL_MIN_WIDTH).clipToBounds(),
                ) { (_, keyLabel) ->
                    val keyDescription = stringResource(Res.string.cd_key, keyLabel)
                    Text(
                        keyLabel,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics {
                            contentDescription = keyDescription
                        },
                    )
                }
                IconButton(
                    onClick = { onEvent(SongDetailEvent.TransposeUpClicked) },
                    modifier = Modifier.semantics { contentDescription = transposeUp },
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics {})
                }
            }
        }
        state.originalKeyLabel?.let {
            TextButton(onClick = { onEvent(SongDetailEvent.TransposeResetClicked) }, modifier = Modifier.heightIn(min = Sizes.touchTarget)) {
                Text(stringResource(Res.string.song_original_key, it) + " · " + stringResource(Res.string.song_transpose_reset))
            }
        }
        NotationSwitch(state.notation, onSelect = { onEvent(SongDetailEvent.NotationSelected(it)) }, Modifier.padding(top = Spacing.m))
    }
}

@Composable
private fun NotationSwitch(selected: Notation, onSelect: (Notation) -> Unit, modifier: Modifier = Modifier) {
    val notations = listOf(
        Notation.NASHVILLE to Res.string.notation_nashville,
        Notation.SOLFEGE to Res.string.notation_solfege,
        Notation.LETTERS to Res.string.notation_letters,
    )
    val shape = RoundedCornerShape(Radius.card)
    Row(
        modifier
            .fillMaxWidth()
            .height(Sizes.touchTarget)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(Sizes.hairline, MaterialTheme.colorScheme.outlineVariant, shape)
            .selectableGroup(),
    ) {
        notations.forEach { (notation, label) ->
            val isSelected = notation == selected
            // The gradient fades from the old choice to the new one rather than jumping.
            val highlight by animateFloatAsState(if (isSelected) 1f else 0f, tween(220), label = "notation-highlight")
            val textColor by animateColorAsState(
                if (isSelected) BrandColors.onGradient else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(220),
                label = "notation-text",
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(BrandColors.horizontalGradient, shape, alpha = highlight)
                    .selectable(selected = isSelected, onClick = { onSelect(notation) }, role = Role.Tab),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    maxLines = 1,
                )
            }
        }
    }
}
