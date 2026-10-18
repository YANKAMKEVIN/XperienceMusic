package com.xperiencemusic.shared.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.music.pitch.*
import com.xperiencemusic.shared.domain.learning.PlayLearningExample
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.repository.*
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.PlayLocalAudioUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.abs
import kotlin.math.sqrt

enum class PianoScalePhase { READY, PREPARING, COUNTDOWN, RECORDING, ANALYZING, RESULT }
enum class PianoInputSignal { QUIET, SOUND, LOUD }
data class PianoScaleUiState(
    val allowed: Boolean = false,
    val phase: PianoScalePhase = PianoScalePhase.READY,
    val practice: PianoScalePractice = PianoScalePractice(),
    val seconds: Double = 0.0,
    val level: Float = 0f,
    val signal: PianoInputSignal = PianoInputSignal.QUIET,
    val signalSeen: Boolean = false,
    val countdown: Int = 3,
    val report: PianoScaleReport? = null,
    val comparison: PianoScaleComparison? = null,
    val selectedStep: Int? = null,
    val preparingReplay: Boolean = false,
    val message: String? = null,
) {
    val busy get() = phase !in listOf(PianoScalePhase.READY, PianoScalePhase.RESULT)
    val visualBeat get() = (seconds * practice.bpm / 60).toInt().takeIf { it in practice.notes.indices }
}

/** One bounded local take plus a report baseline; no upload or skill validation. */
class PianoScaleViewModel(
    observeMember: ObserveCurrentMemberUseCase,
    private val microphone: MicrophoneInput,
    private val example: PlayLearningExample,
    private val preview: PlayLocalAudioUseCase,
    private val files: FileStore,
    private val analysisDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PianoScaleUiState())
    val state = mutableState.asStateFlow()
    val playback get() = preview.state
    private var capture: Job? = null
    private var analysis: Job? = null
    private var stage: Job? = null
    private var replay: Job? = null
    private val chunks = mutableListOf<FloatArray>()
    private var sampleRate = 0
    private var samplesCount = 0
    private var pcm: FloatArray? = null
    private var wav: ByteArray? = null
    private var baseline: PianoScaleReport? = null
    private var signalSeconds = 0.0
    private var recordingPrimed = false
    private var generation = 0
    private var foreground = true
    private var memberId: String? = null

    init {
        observeMember().onEach { member ->
            if (memberId != member?.id || member?.profile?.instrument != Instrument.PIANO) {
                val active = foreground
                interrupt()
                foreground = active
            }
            memberId = member?.id
            mutableState.update { it.copy(allowed = member?.profile?.instrument == Instrument.PIANO) }
        }.launchIn(viewModelScope)
    }
    fun activate() { foreground = true }
    fun denied() { if (foreground) mutableState.update { it.copy(message = "Autorise le microphone pour essayer la correction.") } }
    fun configure(practice: PianoScalePractice) {
        if (state.value.busy || state.value.practice == practice) return
        generation++
        clearAudio()
        baseline = null
        mutableState.value = PianoScaleUiState(allowed = state.value.allowed, practice = practice)
    }
    fun focusStep(index: Int) {
        val s = state.value
        val report = s.report ?: return
        if (s.busy || report.quality != PianoScaleQuality.USABLE) return
        val expected = report.expectedIndexAt(index) ?: return
        configure(s.practice.focus(expected))
    }
    fun selectStep(index: Int) {
        if (state.value.report?.steps?.indices?.contains(index) == true && !state.value.busy)
            mutableState.update { it.copy(selectedStep = index) }
    }

    /** Opens the mic for a disposable signal check; calibration/countdown audio is never stored. */
    fun start() {
        if (!foreground || !state.value.allowed || state.value.busy) return
        state.value.report?.takeIf { it.quality == PianoScaleQuality.USABLE }?.let { baseline = it }
        clearAudio()
        val started = ++generation
        mutableState.value = PianoScaleUiState(allowed = true, phase = PianoScalePhase.PREPARING, practice = state.value.practice)
        stage = viewModelScope.launch {
            delay(20_000)
            cancelPreparation("Le test micro est terminé. Rapproche le téléphone et réessaie quand tu es prêt.")
        }
        val previous = capture
        capture = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                previous?.cancelAndJoin()
                microphone.frames(4096).collect { frame ->
                    if (started != generation) return@collect
                    require(frame.sampleRate in 8_000..192_000 && frame.samples.isNotEmpty() && frame.samples.all { it.isFinite() })
                    if (sampleRate != 0) require(sampleRate == frame.sampleRate)
                    sampleRate = frame.sampleRate
                    val rms = sqrt(frame.samples.sumOf { it.toDouble() * it } / frame.samples.size)
                    val signal = when {
                        frame.samples.count { abs(it) >= 0.98f }.toDouble() / frame.samples.size > 0.02 -> PianoInputSignal.LOUD
                        rms >= 0.008 -> PianoInputSignal.SOUND
                        else -> PianoInputSignal.QUIET
                    }
                    if (signal == PianoInputSignal.SOUND) signalSeconds += frame.samples.size.toDouble() / sampleRate
                    mutableState.update { it.copy(level = (rms * 6).toFloat().coerceIn(0f, 1f), signal = signal, signalSeen = signalSeconds >= 0.15) }
                    if (state.value.phase == PianoScalePhase.RECORDING) {
                        // The first delivered frame can straddle the countdown boundary.
                        if (!recordingPrimed) { recordingPrimed = true; return@collect }
                        val remaining = MAX_SECONDS * sampleRate - samplesCount
                        if (remaining > 0) {
                            val part = frame.samples.copyOf(minOf(frame.samples.size, remaining))
                            chunks += part
                            samplesCount += part.size
                            mutableState.update { it.copy(seconds = samplesCount.toDouble() / sampleRate) }
                        }
                        if (samplesCount >= MAX_SECONDS * sampleRate) stop()
                    }
                }
                if (started == generation) {
                    if (state.value.phase == PianoScalePhase.RECORDING) stop()
                    else cancelPreparation("Le microphone s’est arrêté. Réessaie le test micro.")
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (started == generation) failCapture("Microphone indisponible ou prise interrompue. Vérifie l’autorisation et réessaie.")
            }
        }
        capture?.start()
    }
    fun beginCountdown() {
        val s = state.value
        if (!foreground || s.phase != PianoScalePhase.PREPARING || !s.signalSeen || s.signal == PianoInputSignal.LOUD) return
        stage?.cancel()
        mutableState.update { it.copy(phase = PianoScalePhase.COUNTDOWN, countdown = 3, message = null) }
        stage = viewModelScope.launch {
            for (count in 3 downTo 1) { mutableState.update { it.copy(countdown = count) }; delay(1_000) }
            mutableState.update { it.copy(phase = PianoScalePhase.RECORDING, seconds = 0.0) }
            delay(MAX_SECONDS * 1_000L)
            stop()
        }
    }
    fun cancelPreparation(message: String? = null) {
        if (state.value.phase !in listOf(PianoScalePhase.PREPARING, PianoScalePhase.COUNTDOWN)) return
        failCapture(message)
    }
    private fun failCapture(message: String?) {
        generation++
        capture?.cancel(); stage?.cancel()
        clearAudio()
        mutableState.value = PianoScaleUiState(allowed = state.value.allowed, practice = state.value.practice, message = message)
    }
    fun stop() {
        if (state.value.phase != PianoScalePhase.RECORDING) return
        stage?.cancel()
        mutableState.update { it.copy(phase = PianoScalePhase.ANALYZING, level = 0f) }
        val recording = capture
        recording?.cancel()
        val started = generation
        val targets = state.value.practice.notes
        analysis = viewModelScope.launch {
            try {
                recording?.join()
                val samples = FloatArray(samplesCount)
                var offset = 0
                chunks.forEach { it.copyInto(samples, offset); offset += it.size }
                chunks.clear()
                val rate = sampleRate
                val (report, audio) = withContext(analysisDispatcher) {
                    val context = currentCoroutineContext()
                    val result = PianoScaleAnalyzer().analyze(samples, rate, targets) { context.ensureActive() }
                    val encoded = if (samples.isNotEmpty() && rate > 0) WavEncoder.encode(samples, rate, maxSampleRate = 24_000) else null
                    result to encoded
                }
                if (started == generation && foreground) {
                    pcm = samples
                    wav = audio
                    val selected = report.steps.indexOfFirst { it.correction != PianoNoteCorrection.MATCH }.let { if (it < 0) 0 else it }
                    mutableState.update { it.copy(phase = PianoScalePhase.RESULT, report = report,
                        comparison = comparePianoScales(baseline, report), selectedStep = selected.takeIf { report.steps.isNotEmpty() }) }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (started == generation) mutableState.update { it.copy(phase = PianoScalePhase.READY, message = "Analyse impossible. Recommence une courte prise.") }
            }
        }
    }
    fun listenExample() {
        if (foreground && state.value.allowed && !state.value.busy) {
            stopPlayback()
            example.sequence(state.value.practice.notes, state.value.practice.bpm)
        }
    }
    fun listenExpected(index: Int) {
        if (!foreground || state.value.busy) return
        val note = state.value.report?.steps?.getOrNull(index)?.expected ?: return
        stopPlayback()
        example.sequence(listOf(note), state.value.practice.bpm)
    }
    fun listenTake() {
        if (!foreground || state.value.phase != PianoScalePhase.RESULT) return
        stopPlayback()
        wav?.let { preview(PREVIEW_PATH, it, NowPlaying("Mon essai au piano", "Essai privé sur ce téléphone")) }
    }
    fun listenPassage(index: Int) {
        val s = state.value
        val samples = pcm ?: return
        if (!foreground || s.phase != PianoScalePhase.RESULT || s.preparingReplay || sampleRate <= 0) return
        val range = s.report?.passage(index, samples.size.toDouble() / sampleRate) ?: return
        stopPlayback()
        val started = generation
        val rate = sampleRate
        mutableState.update { it.copy(preparingReplay = true, selectedStep = index) }
        replay = viewModelScope.launch {
            try {
                val audio = withContext(analysisDispatcher) {
                    val first = (range.startSeconds * rate).toInt().coerceIn(0, samples.lastIndex)
                    val last = (range.endSeconds * rate).toInt().coerceIn(first + 1, samples.size)
                    WavEncoder.encode(samples.copyOfRange(first, last), rate, maxSampleRate = 24_000)
                }
                if (started == generation && foreground) preview(PASSAGE_PATH, audio, NowPlaying("Passage à réécouter", "Repère approximatif"))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (started == generation) mutableState.update { it.copy(message = "Lecture du passage impossible. Réessaie la réécoute.") }
            } finally { if (started == generation) mutableState.update { it.copy(preparingReplay = false) } }
        }
    }
    fun stopPlayback() {
        replay?.cancel()
        example.stop()
        preview.stop()
        mutableState.update { it.copy(preparingReplay = false) }
    }
    fun interrupt() {
        foreground = false
        generation++
        capture?.cancel(); analysis?.cancel(); stage?.cancel()
        val interrupted = state.value.busy
        clearAudio()
        baseline = null
        mutableState.value = PianoScaleUiState(allowed = state.value.allowed, practice = state.value.practice,
            message = if (interrupted) "L’essai a été interrompu. Recommence quand tu es prêt." else null)
    }
    private fun clearAudio() {
        stopPlayback()
        files.delete(PREVIEW_PATH); files.delete(PASSAGE_PATH)
        chunks.clear(); sampleRate = 0; samplesCount = 0; signalSeconds = 0.0
        pcm = null; wav = null; recordingPrimed = false
    }
    override fun onCleared() { interrupt() }
    companion object {
        const val MAX_SECONDS = 30
        private const val PREVIEW_PATH = "voice/piano-scale-coach.wav"
        private const val PASSAGE_PATH = "voice/piano-scale-passage.wav"
    }
}
