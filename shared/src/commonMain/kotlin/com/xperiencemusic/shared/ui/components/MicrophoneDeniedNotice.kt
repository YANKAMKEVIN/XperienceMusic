package com.xperiencemusic.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.microphone_open_settings
import com.xperiencemusic.shared.resources.microphone_settings_hint
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * The microphone was refused. After a refusal the phone no longer asks again, so the way out is
 * the app's page in the phone settings, one tap away.
 */
@Composable
fun MicrophoneDeniedNotice(
    message: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        horizontalAlignment = if (textAlign == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            Text(message, color = MaterialTheme.colorScheme.error, textAlign = textAlign)
            Text(
                stringResource(Res.string.microphone_settings_hint),
                style = MaterialTheme.typography.bodySmall,
                textAlign = textAlign,
            )
        }
        OutlinedButton(onClick = onOpenSettings) { Text(stringResource(Res.string.microphone_open_settings)) }
    }
}

@Preview
@Composable
private fun MicrophoneDeniedNoticePreview() = AppTheme {
    MicrophoneDeniedNotice("Autorise l’accès au micro dans les réglages du téléphone pour faire cet exercice.", onOpenSettings = {})
}
