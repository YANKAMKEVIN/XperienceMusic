package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.FileStore
import com.xperiencemusic.shared.domain.repository.NowPlaying

/** Plays audio that only exists on the phone, like a recording not sent yet or a synthesized tone. */
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

    /** Plays the file at [path], rendering it with [render] only the first time. */
    fun playCached(path: String, nowPlaying: NowPlaying, render: () -> ByteArray) {
        playback.stop()
        player.play(files.fileUrl(path) ?: files.write(path, render()), nowPlaying)
    }

    fun pause() = player.pause()

    fun resume() {
        if (player.state.value.hasEnded) player.seekTo(0)
        player.resume()
    }

    fun stop() = player.stop()

    /** Erases private takes from the phone once they are no longer offered. */
    fun discard(vararg paths: String) = paths.forEach(files::delete)
}
