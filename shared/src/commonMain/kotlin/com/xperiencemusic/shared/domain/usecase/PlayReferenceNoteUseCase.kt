package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.music.pitch.ToneGenerator
import com.xperiencemusic.shared.domain.repository.NowPlaying

/** Plays the target note as a pure tone so the singer can find it before singing. */
class PlayReferenceNoteUseCase(private val audio: PlayLocalAudioUseCase) {
    /** [seconds] long enough to sing over it, for harmony exercises. */
    operator fun invoke(midi: Int, seconds: Int = 2) {
        audio.playCached("tones/$midi-$seconds.wav", NowPlaying(title = Pitch.noteLabel(midi), subtitle = "XperienceMusic")) {
            ToneGenerator.wav(midi, seconds.toDouble())
        }
    }
}
