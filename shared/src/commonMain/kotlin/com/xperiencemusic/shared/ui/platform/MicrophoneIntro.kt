package com.xperiencemusic.shared.ui.platform

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.tooling.preview.Preview
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.microphone_intro_continue
import com.xperiencemusic.shared.resources.microphone_intro_later
import com.xperiencemusic.shared.resources.microphone_intro_listen_piano
import com.xperiencemusic.shared.resources.microphone_intro_listen_voice
import com.xperiencemusic.shared.resources.microphone_intro_record
import com.xperiencemusic.shared.resources.microphone_intro_title
import com.xperiencemusic.shared.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** What the microphone is for on a screen: the explanation before the first prompt says it. */
enum class MicrophoneUse {
    /** Live pitch of the voice, nothing kept (exercises, sung activities). */
    LISTEN_VOICE,

    /** Live notes of the piano, nothing kept (the scale workshop). */
    LISTEN_PIANO,

    /** A take the member listens to, then chooses to send (Ma voix, recording for the leader). */
    RECORD,
}

/** Whether the microphone was already explained on this phone, and a way to say it now is. */
interface MicrophoneIntro {
    fun wasExplained(): Boolean

    fun markExplained()
}

/** Provided once at the root of the app; null (previews, tests) never shows the explanation. */
val LocalMicrophoneIntro = staticCompositionLocalOf<MicrophoneIntro?> { null }

/**
 * The microphone permission, with a short explanation before the very first system prompt
 * ("rien n’est enregistré sans toi"). Afterwards, or once granted, [MicrophonePermission.request]
 * goes straight to the phone. The explanation dialog is shown by this call while it is pending.
 */
@Composable
fun rememberMicrophonePermission(use: MicrophoneUse): MicrophonePermission {
    val system = rememberMicrophonePermission()
    val intro = LocalMicrophoneIntro.current
    var pending by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    pending?.let { onResult ->
        MicrophoneIntroDialog(
            use = use,
            onContinue = {
                pending = null
                intro?.markExplained()
                system.request(onResult)
            },
            // Nothing was asked: the explanation comes back with the next tap.
            onLater = { pending = null },
        )
    }
    return MicrophonePermission(
        isGranted = system.isGranted,
        request = { onResult ->
            if (system.isGranted || intro == null || intro.wasExplained()) system.request(onResult) else pending = onResult
        },
        openSettings = system.openSettings,
    )
}

@Composable
private fun MicrophoneIntroDialog(use: MicrophoneUse, onContinue: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(Res.string.microphone_intro_title)) },
        text = {
            Text(
                stringResource(
                    when (use) {
                        MicrophoneUse.LISTEN_VOICE -> Res.string.microphone_intro_listen_voice
                        MicrophoneUse.LISTEN_PIANO -> Res.string.microphone_intro_listen_piano
                        MicrophoneUse.RECORD -> Res.string.microphone_intro_record
                    },
                ),
            )
        },
        confirmButton = { TextButton(onClick = onContinue) { Text(stringResource(Res.string.microphone_intro_continue)) } },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(Res.string.microphone_intro_later)) } },
    )
}

@Preview
@Composable
private fun MicrophoneIntroDialogPreview() = AppTheme {
    MicrophoneIntroDialog(MicrophoneUse.LISTEN_VOICE, onContinue = {}, onLater = {})
}
