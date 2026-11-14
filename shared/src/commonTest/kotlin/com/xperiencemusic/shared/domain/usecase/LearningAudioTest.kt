package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.pitch.PianoPassage
import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.music.pitch.WavEncoder
import com.xperiencemusic.shared.domain.repository.AudioFrame
import com.xperiencemusic.shared.domain.repository.MicrophoneInput
import com.xperiencemusic.shared.domain.repository.NowPlaying
import com.xperiencemusic.shared.fakes.FakeAudioPlayer
import com.xperiencemusic.shared.fakes.FakeFileStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LearningAudioTest {
    private val files = FakeFileStore()
    private val player = FakeAudioPlayer()

    private fun microphone(vararg frames: AudioFrame) = object : MicrophoneInput {
        override fun frames(frameSize: Int): Flow<AudioFrame> = flowOf(*frames)
    }

    @Test
    fun examplesAreSynthesizedOnThePhoneAndPlayedOneAtATime() = runTest {
        val playback = PlaybackController(player, { it }, backgroundScope)
        val examples = PlayLearningExampleUseCase(PlayLocalAudioUseCase(files, player, playback), playback)
        examples(listOf(60, 64, 67))
        examples.broken(listOf(60, 64))
        examples.metronome(72)
        assertEquals(
            setOf("tones/learning-60-64-67-false-1.4.wav", "tones/learning-60-64-true-0.5.wav", "tones/click-72.wav"),
            files.files.keys,
        )
        assertEquals(3, player.played.size)
        assertEquals("Exemple musical", player.nowPlaying.last().title)
        examples(emptyList())
        assertEquals(3, player.played.size, "nothing to play")
    }

    @Test
    fun aReferenceNoteIsRenderedOnlyOnce() = runTest {
        val playback = PlaybackController(player, { it }, backgroundScope)
        val reference = PlayReferenceNoteUseCase(PlayLocalAudioUseCase(files, player, playback))
        reference(69)
        val first = files.files.getValue("tones/69-2.wav")
        reference(69)
        assertTrue(first === files.files.getValue("tones/69-2.wav"))
        assertEquals(2, player.played.size)
        assertEquals(Pitch.noteLabel(69), player.nowPlaying.last().title)
    }

    @Test
    fun theRecorderKeepsWhatItIsAskedAndStopsAtTheLimit() = runTest {
        val frame = AudioFrame(FloatArray(4) { 0.5f }, 8_000)
        val recorder = PcmRecorder(microphone(frame, frame, frame))
        recorder.frames().toList().forEach { recorder.keep(it, maxSamples = 10) }
        assertEquals(10, recorder.sampleCount)
        assertEquals(3, recorder.frameCount)
        assertEquals(10, recorder.samples().size)
        recorder.clear()
        assertEquals(0, recorder.sampleCount)
        assertEquals(0, recorder.sampleRate)
    }

    @Test
    fun aStrictCaptureRefusesAStreamThatChangesRate() = runTest {
        val recorder = PcmRecorder(microphone(AudioFrame(FloatArray(4), 8_000), AudioFrame(FloatArray(4), 16_000)))
        assertFailsWith<IllegalArgumentException> { recorder.frames(strict = true).toList() }
        assertEquals(2, PcmRecorder(microphone(AudioFrame(FloatArray(4), 8_000), AudioFrame(FloatArray(4), 16_000))).frames().toList().size)
    }

    @Test
    fun framesAreMeasured() {
        val loud = CapturedFrame(FloatArray(100) { 1f }, 8_000)
        assertEquals(1.0, loud.clippedShare)
        assertEquals(1f, loud.level)
        val quiet = CapturedFrame(FloatArray(100) { 0.01f }, 8_000)
        assertEquals(0.0, quiet.clippedShare)
        assertEquals(0.01, quiet.loudness, 1e-6)
    }

    @Test
    fun aPianoTakeIsAnalysedAndAPassageCutOut() = runTest {
        val rate = 12_000
        val samples = FloatArray(rate * 2) { i -> (sin(2 * PI * 440 * i / rate) * 0.3).toFloat() }
        val analyze = AnalyzePianoScaleUseCase(UnconfinedTestDispatcher(testScheduler))
        val take = analyze(samples, rate, listOf(69, 71, 72))
        assertEquals(2.0, take.seconds)
        assertTrue(take.wav!!.size > WavEncoder.HEADER_SIZE)
        val passage = analyze.passage(take, PianoPassage(0.5, 1.0))
        assertEquals(WavEncoder.HEADER_SIZE + rate / 2 * 2, passage.size)
        assertTrue(analyze(FloatArray(0), 0, listOf(69, 71, 72)).wav == null)
    }
}
