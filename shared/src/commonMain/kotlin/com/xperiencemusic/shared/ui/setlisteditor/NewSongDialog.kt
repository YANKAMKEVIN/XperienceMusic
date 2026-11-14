package com.xperiencemusic.shared.ui.setlisteditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.layout.padding
import com.xperiencemusic.shared.presentation.setlisteditor.NewSongError
import com.xperiencemusic.shared.presentation.setlisteditor.NewSongUi
import com.xperiencemusic.shared.presentation.setlisteditor.SetlistEditorEvent
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.editor_error_title
import com.xperiencemusic.shared.resources.editor_title_label
import com.xperiencemusic.shared.resources.error_forbidden
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.setlist_new_song
import com.xperiencemusic.shared.resources.setlist_new_song_add
import com.xperiencemusic.shared.resources.setlist_new_song_cancel
import com.xperiencemusic.shared.resources.setlist_new_song_from_search
import com.xperiencemusic.shared.resources.setlist_new_song_help
import com.xperiencemusic.shared.resources.youtube_link_invalid
import com.xperiencemusic.shared.ui.components.YouTubeLinkField
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Under the search: a song missing from the library is created right here, from its title. */
@Composable
internal fun NewSongButton(query: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = query.trim()
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = Sizes.touchTarget)) {
        Icon(Icons.Filled.Add, contentDescription = null)
        val label = if (title.isEmpty()) {
            stringResource(Res.string.setlist_new_song)
        } else {
            stringResource(Res.string.setlist_new_song_from_search, title)
        }
        Text(
            label,
            modifier = Modifier.padding(start = Spacing.s),
        )
    }
}

/** A title and its YouTube video, as the WhatsApp message gave them. */
@Composable
internal fun NewSongDialog(form: NewSongUi, onEvent: (SetlistEditorEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(SetlistEditorEvent.NewSongDismissed) },
        title = { Text(stringResource(Res.string.setlist_new_song)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(stringResource(Res.string.setlist_new_song_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = form.title,
                    onValueChange = { onEvent(SetlistEditorEvent.NewSongTitleChanged(it)) },
                    label = { Text(stringResource(Res.string.editor_title_label)) },
                    isError = form.error == NewSongError.MISSING_TITLE,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                YouTubeLinkField(
                    value = form.youtubeUrl,
                    onValueChange = { onEvent(SetlistEditorEvent.NewSongLinkChanged(it)) },
                    isError = form.error == NewSongError.INVALID_YOUTUBE_LINK,
                    imeAction = ImeAction.Done,
                )
                form.error?.let { Text(stringResource(it.message()), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(SetlistEditorEvent.NewSongConfirmed) }, enabled = form.canConfirm) {
                Text(stringResource(Res.string.setlist_new_song_add))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(SetlistEditorEvent.NewSongDismissed) }, enabled = !form.isSaving) {
                Text(stringResource(Res.string.setlist_new_song_cancel))
            }
        },
    )
}

private fun NewSongError.message() = when (this) {
    NewSongError.MISSING_TITLE -> Res.string.editor_error_title
    NewSongError.INVALID_YOUTUBE_LINK -> Res.string.youtube_link_invalid
    NewSongError.FORBIDDEN -> Res.string.error_forbidden
    NewSongError.NETWORK -> Res.string.error_generic
}
