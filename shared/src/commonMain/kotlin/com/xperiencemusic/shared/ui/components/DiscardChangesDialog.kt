package com.xperiencemusic.shared.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.discard_confirm
import com.xperiencemusic.shared.resources.discard_keep
import com.xperiencemusic.shared.resources.discard_text
import com.xperiencemusic.shared.resources.discard_title
import com.xperiencemusic.shared.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Asked when leaving a form with unsaved changes, from the back arrow or the system back. */
@Composable
fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
    title: String = stringResource(Res.string.discard_title),
    text: String = stringResource(Res.string.discard_text),
    keepLabel: String = stringResource(Res.string.discard_keep),
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(Res.string.discard_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) { Text(keepLabel) }
        },
    )
}

@Preview
@Composable
private fun DiscardChangesDialogPreview() {
    AppTheme { DiscardChangesDialog(onDiscard = {}, onKeepEditing = {}) }
}
