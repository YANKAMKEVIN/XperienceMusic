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
    operator fun invoke(midi: Int) {
        playback.stop()
        val path = "tones/$midi.wav"
        val url = files.fileUrl(path) ?: files.write(path, ToneGenerator.wav(midi))
        player.play(url, NowPlaying(title = Pitch.noteLabel(midi), subtitle = "XperienceMusic"))
    }
}
