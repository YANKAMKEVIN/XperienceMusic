package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.pitch.PianoPassage
import com.xperiencemusic.music.pitch.PianoScaleAnalyzer
import com.xperiencemusic.music.pitch.PianoScaleReport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** A piano take analysed on the phone: its samples for replays, the report and the audio to listen to. */
class PianoTake(
    val samples: FloatArray,
    val sampleRate: Int,
    val report: PianoScaleReport,
    /** Null when nothing was recorded. */
    val wav: ByteArray?,
) {
    val seconds: Double get() = samples.size.toDouble() / sampleRate
}

/** Compares a piano take with the expected notes, off the main thread; the audio never leaves the phone. */
class AnalyzePianoScaleUseCase(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) {
    suspend operator fun invoke(samples: FloatArray, sampleRate: Int, targets: List<Int>): PianoTake = withContext(dispatcher) {
        val context = currentCoroutineContext()
        val report = PianoScaleAnalyzer().analyze(samples, sampleRate, targets) { context.ensureActive() }
        val wav = if (samples.isNotEmpty() && sampleRate > 0) PcmRecorder.wav(samples, sampleRate) else null
        PianoTake(samples, sampleRate, report, wav)
    }

    /** The audio of [passage] alone, to replay one spot of the take. */
    suspend fun passage(take: PianoTake, passage: PianoPassage): ByteArray = withContext(dispatcher) {
        val samples = take.samples
        val first = (passage.startSeconds * take.sampleRate).toInt().coerceIn(0, samples.lastIndex)
        val last = (passage.endSeconds * take.sampleRate).toInt().coerceIn(first + 1, samples.size)
        PcmRecorder.wav(samples.copyOfRange(first, last), take.sampleRate)
    }
}
