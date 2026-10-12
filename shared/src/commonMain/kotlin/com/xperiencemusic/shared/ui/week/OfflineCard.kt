package com.xperiencemusic.shared.ui.week

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.presentation.week.WeekOfflineUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.week_offline_done
import com.xperiencemusic.shared.resources.week_offline_download
import com.xperiencemusic.shared.resources.week_offline_downloading
import com.xperiencemusic.shared.resources.week_offline_error
import com.xperiencemusic.shared.resources.week_offline_partial
import com.xperiencemusic.shared.resources.week_offline_retry
import com.xperiencemusic.shared.resources.week_offline_text
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * One line that lets members keep the coming weeks' recordings on the phone, to rehearse where
 * there is no network; it turns into a quiet confirmation once everything is there.
 */
@Composable
fun OfflineCard(offline: WeekOfflineUi, onDownload: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (offline.isComplete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(Modifier.weight(1f).padding(horizontal = Spacing.m).semantics { liveRegion = LiveRegionMode.Polite }) {
                Text(
                    when {
                        offline.isComplete -> stringResource(Res.string.week_offline_done, offline.total)
                        offline.isDownloading -> stringResource(Res.string.week_offline_downloading, offline.available, offline.total)
                        offline.available > 0 -> stringResource(Res.string.week_offline_partial, offline.available, offline.total)
                        else -> stringResource(Res.string.week_offline_text, offline.total)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (offline.hasError && !offline.isDownloading) {
                    Text(
                        stringResource(Res.string.week_offline_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
            if (!offline.isComplete && !offline.isDownloading) {
                TextButton(onClick = onDownload) {
                    Text(stringResource(if (offline.hasError) Res.string.week_offline_retry else Res.string.week_offline_download))
                }
            }
        }
        if (offline.isDownloading) {
            LinearProgressIndicator(
                progress = { offline.available.toFloat() / offline.total },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.m),
            )
        }
    }
}
