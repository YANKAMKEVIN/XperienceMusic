package com.xperiencemusic.shared.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.youtube_link_hint
import com.xperiencemusic.shared.resources.youtube_link_label
import org.jetbrains.compose.resources.stringResource

/** The song's YouTube video, pasted from the share sheet or a WhatsApp message. */
@Composable
fun YouTubeLinkField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    /** The keyboard's action key, when it should do more than move on (confirm a dialog). */
    onImeAction: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.youtube_link_label)) },
        placeholder = { Text(stringResource(Res.string.youtube_link_hint)) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = imeAction),
        keyboardActions = onImeAction?.let { action -> KeyboardActions(onAny = { action() }) } ?: KeyboardActions.Default,
        modifier = modifier.fillMaxWidth(),
    )
}
