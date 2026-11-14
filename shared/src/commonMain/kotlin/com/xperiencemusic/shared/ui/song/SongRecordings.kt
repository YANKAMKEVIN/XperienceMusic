package com.xperiencemusic.shared.ui.song

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.xperiencemusic.shared.ui.components.PauseIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.xperiencemusic.shared.presentation.song.LinkUi
import com.xperiencemusic.shared.presentation.song.RecordingUi
import com.xperiencemusic.shared.presentation.song.SongDetailEvent
import com.xperiencemusic.shared.presentation.song.VoiceTrackUi
import com.xperiencemusic.shared.presentation.common.PlaybackStatus
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.song_links_title
import com.xperiencemusic.shared.resources.song_recording_actions
import com.xperiencemusic.shared.resources.song_recording_delete
import com.xperiencemusic.shared.resources.song_recording_meta
import com.xperiencemusic.shared.resources.song_recording_offline
import com.xperiencemusic.shared.resources.song_recording_reference
import com.xperiencemusic.shared.resources.song_recording_set_reference
import com.xperiencemusic.shared.resources.song_voice_tracks_title
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.PlayPauseButton
import com.xperiencemusic.shared.ui.components.SectionTitle
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.format.rememberFormattingWords
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RecordingRow(recording: RecordingUi, canManage: Boolean, onEvent: (SongDetailEvent) -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayPauseButton(
                status = recording.playback,
                itemLabel = recording.title,
                onClick = { onEvent(SongDetailEvent.PlayRecordingClicked(recording.id)) },
            )
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (recording.isReference) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = stringResource(Res.string.song_recording_reference),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = Spacing.xs).size(Sizes.iconXs),
                        )
                    }
                    Text(recording.title, style = MaterialTheme.typography.titleSmall)
                }
                val meta = stringResource(Res.string.song_recording_meta, rememberFormattingWords().shortDate(recording.recordedOn), recording.duration)
                val offlineLabel = stringResource(Res.string.song_recording_offline)
                Text(
                    if (recording.isAvailableOffline) "$meta · $offlineLabel" else meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canManage) RecordingMenu(recording, onEvent)
        }
    }
}

@Composable
private fun RecordingMenu(recording: RecordingUi, onEvent: (SongDetailEvent) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.song_recording_actions))
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            if (!recording.isReference) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.song_recording_set_reference)) },
                    onClick = {
                        isExpanded = false
                        onEvent(SongDetailEvent.SetReferenceClicked(recording.id))
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.song_recording_delete), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    isExpanded = false
                    onEvent(SongDetailEvent.DeleteClicked(recording.id))
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VoiceTracks(voiceTracks: List<VoiceTrackUi>, onEvent: (SongDetailEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionTitle(stringResource(Res.string.song_voice_tracks_title))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            voiceTracks.forEach { track ->
                val label = track.voicePart.label()
                val isActive = track.playback == PlaybackStatus.PLAYING || track.playback == PlaybackStatus.LOADING
                FilterChip(
                    selected = isActive,
                    onClick = { onEvent(SongDetailEvent.PlayVoiceTrackClicked(track.id, label)) },
                    label = { Text(label) },
                    leadingIcon = {
                        Icon(
                            if (track.playback == PlaybackStatus.PLAYING) PauseIcon else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(Sizes.iconS),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        iconColor = MaterialTheme.colorScheme.primary,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = if (track.playback == PlaybackStatus.PLAYING) "Pause $label" else "Écouter $label"
                    },
                )
            }
        }
    }
}

@Composable
internal fun Links(links: List<LinkUi>) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionTitle(stringResource(Res.string.song_links_title))
        links.forEach { link ->
            TextButton(onClick = { uriHandler.openUri(link.url) }) { Text(link.label) }
        }
    }
}
