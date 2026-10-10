package com.xperiencemusic.shared.ui.week

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.xperiencemusic.shared.presentation.week.WeekOfflineUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.week_offline_done
import com.xperiencemusic.shared.resources.week_offline_download
import com.xperiencemusic.shared.resources.week_offline_downloading
import com.xperiencemusic.shared.resources.week_offline_error
import com.xperiencemusic.shared.resources.week_offline_partial
import com.xperiencemusic.shared.resources.week_offline_retry
import com.xperiencemusic.shared.resources.week_offline_text
import com.xperiencemusic.shared.resources.week_offline_title
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Lets members keep the week's recordings on the phone, to rehearse where there is no network. */
@Composable
fun OfflineCard(offline: WeekOfflineUi, onDownload: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        if (offline.isComplete) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Text(
                    stringResource(Res.string.week_offline_done, offline.total),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Spacing.s),
                )
            }
            return@AppCard
        }
        Text(stringResource(Res.string.week_offline_title), style = MaterialTheme.typography.titleMedium)
        Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            Text(
                when {
                    offline.isDownloading -> stringResource(Res.string.week_offline_downloading, offline.available, offline.total)
                    offline.available > 0 -> stringResource(Res.string.week_offline_partial, offline.available, offline.total)
                    else -> stringResource(Res.string.week_offline_text, offline.total)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
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
        if (offline.isDownloading) {
            LinearProgressIndicator(
                progress = { offline.available.toFloat() / offline.total },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.m),
            )
        } else {
            FilledTonalButton(onClick = onDownload, modifier = Modifier.padding(top = Spacing.m)) {
                Text(stringResource(if (offline.hasError) Res.string.week_offline_retry else Res.string.week_offline_download))
            }
        }
    }
}
