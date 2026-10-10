package com.xperiencemusic.shared.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.PlaybackState
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.presentation.common.Formatting
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MiniPlayerViewModel(private val playback: PlaybackController) : ViewModel() {
    val state: StateFlow<MiniPlayerUiState> = playback.state
        .map { it.toMiniPlayerUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MiniPlayerUiState())

    fun onEvent(event: MiniPlayerEvent) {
        when (event) {
            MiniPlayerEvent.PlayPauseClicked -> playback.togglePause()
            is MiniPlayerEvent.Seeked -> {
                val duration = playback.state.value.durationMs
                if (duration > 0) playback.seekTo((duration * event.fraction.coerceIn(0f, 1f)).toLong())
            }
            MiniPlayerEvent.CloseClicked -> playback.stop()
        }
    }
}

fun PlaybackState.toMiniPlayerUiState(): MiniPlayerUiState {
    val current = track ?: return MiniPlayerUiState()
    return MiniPlayerUiState(
        isVisible = true,
        title = current.title,
        subtitle = current.subtitle,
        isPlaying = isPlaying,
        isLoading = isLoading,
        hasError = hasError,
        progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
        positionLabel = Formatting.duration((positionMs / 1000).toInt()),
        durationLabel = if (durationMs > 0) Formatting.duration((durationMs / 1000).toInt()) else "",
    )
}
