package com.xperiencemusic.shared.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.search_clear
import org.jetbrains.compose.resources.stringResource

/** The × at the end of a search field; 48 dp, named for TalkBack. Pass it as the field's `trailingIcon` only while there is text. */
@Composable
fun ClearSearchButton(onClear: () -> Unit) {
    IconButton(onClick = onClear) {
        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.search_clear))
    }
}
