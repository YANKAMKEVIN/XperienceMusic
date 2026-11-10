package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.pitch.ToneGenerator
import com.xperiencemusic.shared.domain.repository.NowPlaying

/** The guided lessons' examples, synthesized on the phone; short key sounds never play a reward over the exercise. */
class PlayLearningExampleUseCase(
    private val audio: PlayLocalAudioUseCase,
    private val playback: PlaybackController,
) {
    /** [notes] together, as a chord. */
    operator fun invoke(notes: List<Int>) = chord(notes, CHORD_SECONDS)

    fun key(midi: Int) = chord(listOf(midi), KEY_SECONDS)

    /** [notes] one after the other. */
    fun broken(notes: List<Int>) = arpeggio(notes, BROKEN_SECONDS)

    fun sequence(notes: List<Int>, bpm: Int) {
        require(bpm in 40..120)
        arpeggio(notes, 60.0 / bpm)
    }

    fun reward() = arpeggio(REWARD, REWARD_SECONDS)

    fun metronome(bpm: Int) = output("click-$bpm", ToneGenerator.clickWav(bpm))

    fun stop() = playback.stop()

    private fun chord(notes: List<Int>, seconds: Double) {
        if (notes.isEmpty()) return
        output(id(notes, false, seconds), ToneGenerator.chordWav(notes, seconds))
    }

    private fun arpeggio(notes: List<Int>, seconds: Double) {
        if (notes.isEmpty()) return
        output(id(notes, true, seconds), ToneGenerator.arpeggioWav(notes, seconds))
    }

    private fun id(notes: List<Int>, broken: Boolean, seconds: Double) = "learning-${notes.joinToString("-")}-$broken-$seconds"

    private fun output(id: String, bytes: ByteArray) = audio("tones/$id.wav", bytes, NowPlaying("Exemple musical", "XperienceMusic"))

    private companion object {
        const val CHORD_SECONDS = 1.4
        const val KEY_SECONDS = 0.35
        const val BROKEN_SECONDS = 0.5
        const val REWARD_SECONDS = 0.13
        val REWARD = listOf(72, 76, 79)
    }
}
