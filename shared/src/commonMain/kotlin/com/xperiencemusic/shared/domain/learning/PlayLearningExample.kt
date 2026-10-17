package com.xperiencemusic.shared.domain.learning

import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.music.pitch.WavEncoder
import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.FileStore
import com.xperiencemusic.shared.domain.repository.NowPlaying
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Neutral synthesized examples, identical on both platforms; not sampled piano recordings. */
class PlayLearningExample(
    private val files: FileStore,
    private val player: AudioPlayer,
    private val playback: PlaybackController,
) {
    operator fun invoke(notes: List<Int>) {
        if (notes.isEmpty()) return
        playback.stop()
        val rate = 24_000
        val samples = FloatArray(rate * 2) { i ->
            val envelope = min(1.0, min(i, rate * 2 - i - 1) / (rate * 0.03))
            (notes.sumOf { midi -> sin(2 * PI * Pitch.midiToFrequency(midi.toDouble()) * i / rate) }
                / notes.size * 0.35 * envelope).toFloat()
        }
        val path = "tones/learning-${notes.joinToString("-")}.wav"
        val url = files.fileUrl(path) ?: files.write(path, WavEncoder.encode(samples, rate))
        player.play(url, NowPlaying("Exemple musical", "Accords de base"))
    }
    fun stop() = playback.stop()
}
