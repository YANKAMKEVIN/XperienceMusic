package com.xperiencemusic.shared.fakes

import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.AudioPlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Records commands and lets tests move the playback position by hand. */
class FakeAudioPlayer : AudioPlayer {
    override val state = MutableStateFlow(AudioPlayerState())
    val played = mutableListOf<String>()
    var stopCount = 0

    override fun play(url: String) {
        played += url
        state.value = AudioPlayerState(isPlaying = true, durationMs = 200_000)
    }

    override fun pause() = state.update { it.copy(isPlaying = false) }

    override fun resume() = state.update { it.copy(isPlaying = true, hasEnded = false) }

    override fun seekTo(positionMs: Long) = state.update { it.copy(positionMs = positionMs, hasEnded = false) }

    override fun stop() {
        stopCount++
        state.value = AudioPlayerState()
    }

    fun finish() = state.update { it.copy(isPlaying = false, hasEnded = true, positionMs = it.durationMs) }
}
