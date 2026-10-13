package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.FileStore
import com.xperiencemusic.shared.domain.repository.NowPlaying

/** Plays audio that only exists on the phone, like a recording not sent yet. */
class PlayLocalAudioUseCase(
    private val files: FileStore,
    private val player: AudioPlayer,
    private val playback: PlaybackController,
) {
    val state get() = player.state

    operator fun invoke(path: String, bytes: ByteArray, nowPlaying: NowPlaying) {
        playback.stop()
        player.play(files.write(path, bytes), nowPlaying)
    }

    fun pause() = player.pause()

    fun resume() {
        if (player.state.value.hasEnded) player.seekTo(0)
        player.resume()
    }

    fun stop() = player.stop()
}
