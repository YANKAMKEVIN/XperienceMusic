package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.music.pitch.ToneGenerator
import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.FileStore
import com.xperiencemusic.shared.domain.repository.NowPlaying

/** Plays the target note as a pure tone so the singer can find it before singing. */
class PlayReferenceNoteUseCase(
    private val files: FileStore,
    private val player: AudioPlayer,
    private val playback: PlaybackController,
) {
    /** [seconds] long enough to sing over it, for harmony exercises. */
    operator fun invoke(midi: Int, seconds: Int = 2) {
        playback.stop()
        val path = "tones/$midi-$seconds.wav"
        val url = files.fileUrl(path) ?: files.write(path, ToneGenerator.wav(midi, seconds.toDouble()))
        player.play(url, NowPlaying(title = Pitch.noteLabel(midi), subtitle = "XperienceMusic"))
    }
}
