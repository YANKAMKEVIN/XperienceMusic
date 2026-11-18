package com.xperiencemusic.shared.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.offline_banner
import com.xperiencemusic.shared.resources.offline_retry
import com.xperiencemusic.shared.resources.offline_retrying
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Radius
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * Says the server is out of reach and the phone's copy is shown. With [onRetry], a "Réessayer"
 * button sits at its end: pulling the list down still works, but nothing on screen said so.
 * While [isRetrying], the button is disabled and says it is connecting.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null, isRetrying: Boolean = false) {
    Surface(
        // A quiet notice, not a choice: kept in night blue while chips moved to solid violet.
        color = AppTheme.colors.infoContainer,
        shape = RoundedCornerShape(Radius.card),
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Spacing.l, end = if (onRetry != null) Spacing.xs else Spacing.l),
        ) {
            Text(
                stringResource(Res.string.offline_banner),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.info,
                modifier = Modifier.weight(1f).padding(vertical = Spacing.m),
            )
            if (onRetry != null) {
                TextButton(
                    onClick = onRetry,
                    enabled = !isRetrying,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.padding(start = Spacing.s).heightIn(min = Sizes.touchTarget),
                ) {
                    Text(stringResource(if (isRetrying) Res.string.offline_retrying else Res.string.offline_retry))
                }
            }
        }
    }
}
