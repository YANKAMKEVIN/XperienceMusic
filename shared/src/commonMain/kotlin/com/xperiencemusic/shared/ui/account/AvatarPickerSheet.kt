package com.xperiencemusic.shared.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.domain.model.Avatar
import com.xperiencemusic.shared.domain.model.AvatarPreset
import com.xperiencemusic.shared.presentation.account.AccountUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.avatar_import_photo
import com.xperiencemusic.shared.resources.avatar_preset_label
import com.xperiencemusic.shared.resources.avatar_presets_title
import com.xperiencemusic.shared.resources.avatar_remove
import com.xperiencemusic.shared.resources.avatar_sheet_title
import com.xperiencemusic.shared.ui.components.PresetImage
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Their own photo, or one of the drawn avatars (their role's first), or back to initials. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerSheet(
    state: AccountUiState,
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onPreset: (AvatarPreset) -> Unit,
    onRemove: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        val current = (state.me.avatar as? Avatar.Preset)?.preset
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 72.dp),
            contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = Spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(Res.string.avatar_sheet_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                FilledTonalButton(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(Res.string.avatar_import_photo), modifier = Modifier.padding(start = Spacing.s))
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(Res.string.avatar_presets_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
                )
            }
            items(state.presets, key = { it.name }) { preset -> PresetChoice(preset, isSelected = preset == current, onClick = { onPreset(preset) }) }
            if (state.hasAvatar) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    TextButton(onClick = onRemove, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(stringResource(Res.string.avatar_remove), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = Spacing.s))
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChoice(preset: AvatarPreset, isSelected: Boolean, onClick: () -> Unit) {
    // Avatars of one role are numbered 1 to 4 for screen readers: "Chantre, avatar 3".
    val number = AvatarPreset.entries.filter { it.instrument == preset.instrument }.indexOf(preset) + 1
    val label = "${preset.instrument.label()}, ${stringResource(Res.string.avatar_preset_label, number)}"
    val ring = if (isSelected) Modifier.border(BorderStroke(3.dp, BrandColors.gradient), CircleShape).padding(4.dp) else Modifier.padding(4.dp)
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = label
                selected = isSelected
            }
            .then(ring),
    ) {
        PresetImage(preset, Modifier.aspectRatio(1f).clip(CircleShape))
    }
}
