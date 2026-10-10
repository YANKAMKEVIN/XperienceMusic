package com.xperiencemusic.shared.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.presentation.common.PlaybackStatus
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.player_loading
import com.xperiencemusic.shared.resources.player_pause
import com.xperiencemusic.shared.resources.player_play
import org.jetbrains.compose.resources.stringResource

/** Round play/pause button; [itemLabel] names what is played, for screen readers. */
@Composable
fun PlayPauseButton(status: PlaybackStatus, itemLabel: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconButton(onClick = onClick, modifier = modifier) {
        when (status) {
            PlaybackStatus.LOADING -> Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            }
            PlaybackStatus.PLAYING -> Icon(PauseIcon, contentDescription = stringResource(Res.string.player_pause, itemLabel))
            PlaybackStatus.IDLE, PlaybackStatus.PAUSED ->
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(Res.string.player_play, itemLabel))
        }
    }
}
