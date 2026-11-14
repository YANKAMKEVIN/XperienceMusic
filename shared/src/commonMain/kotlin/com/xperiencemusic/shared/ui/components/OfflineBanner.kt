package com.xperiencemusic.shared.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.offline_banner
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Radius
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Says the server is out of reach and the phone's copy is shown. */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(
        // A quiet notice, not a choice: kept in night blue while chips moved to solid violet.
        color = AppTheme.colors.infoContainer,
        shape = RoundedCornerShape(Radius.card),
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            stringResource(Res.string.offline_banner),
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.info,
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
        )
    }
}
