package com.xperiencemusic.shared.ui.session

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.session_sign_out
import com.xperiencemusic.shared.resources.session_sign_out_confirm_cancel
import com.xperiencemusic.shared.resources.session_sign_out_confirm_text
import com.xperiencemusic.shared.resources.session_sign_out_confirm_title
import com.xperiencemusic.shared.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** "Se déconnecter ?": the sign-out wipes what the phone has not synced yet. */
@Composable
fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.session_sign_out_confirm_title)) },
        text = { Text(stringResource(Res.string.session_sign_out_confirm_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.session_sign_out), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.session_sign_out_confirm_cancel)) }
        },
    )
}

@Preview
@Composable
private fun SignOutDialogPreview() {
    AppTheme { SignOutDialog(onConfirm = {}, onDismiss = {}) }
}
