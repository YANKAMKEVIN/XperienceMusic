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
import kotlin.math.sqrt

enum class PianoScalePhase { READY, RECORDING, ANALYZING, RESULT }
data class PianoScaleUiState(
    val allowed: Boolean = false,
    val phase: PianoScalePhase = PianoScalePhase.READY,
    val seconds: Double = 0.0,
    val level: Float = 0f,
    val report: PianoScaleReport? = null,
    val message: String? = null,
)

/** Short, private, local-only piano exercise. It never writes a learning success or a skill check. */
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
    private var capture: Job? = null
    private var analysis: Job? = null
    private val chunks = mutableListOf<FloatArray>()
    private var sampleRate = 0
    private var samplesCount = 0
    private var wav: ByteArray? = null
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
    fun start() {
        if (!foreground || !state.value.allowed || state.value.phase in listOf(PianoScalePhase.RECORDING, PianoScalePhase.ANALYZING)) return
        clearAudio()
        val started = ++generation
        mutableState.value = PianoScaleUiState(allowed = true, phase = PianoScalePhase.RECORDING)
        val previous = capture
        capture = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                previous?.cancelAndJoin()
                microphone.frames(4096).collect { frame ->
                    if (started != generation) return@collect
                    require(frame.sampleRate in 8_000..192_000 && frame.samples.isNotEmpty())
                    if (sampleRate != 0) require(sampleRate == frame.sampleRate)
                    sampleRate = frame.sampleRate
                    val remaining = MAX_SECONDS * sampleRate - samplesCount
                    val part = frame.samples.copyOf(minOf(frame.samples.size, remaining))
                    chunks += part
                    samplesCount += part.size
                    val level = sqrt(part.sumOf { it.toDouble() * it } / part.size)
                    mutableState.update { it.copy(seconds = samplesCount.toDouble() / sampleRate, level = (level * 6).toFloat().coerceIn(0f, 1f)) }
                    if (samplesCount >= MAX_SECONDS * sampleRate) stop()
                }
                if (state.value.phase == PianoScalePhase.RECORDING) stop()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (started == generation) {
                    clearAudio()
                    mutableState.value = PianoScaleUiState(allowed = state.value.allowed, message = "Microphone indisponible ou prise interrompue. Vérifie l’autorisation et réessaie.")
                }
            }
        }
        capture?.start()
    }

    fun stop() {
        if (state.value.phase != PianoScalePhase.RECORDING) return
        mutableState.update { it.copy(phase = PianoScalePhase.ANALYZING, level = 0f) }
        val recording = capture
        recording?.cancel()
        val started = generation
        analysis = viewModelScope.launch {
            try {
                // Release the hardware before analysis or playback opens an audio session.
                recording?.join()
                val pcm = FloatArray(samplesCount)
                var offset = 0
                chunks.forEach { it.copyInto(pcm, offset); offset += it.size }
                chunks.clear()
                val rate = sampleRate
                val (report, audio) = withContext(analysisDispatcher) {
                    val context = currentCoroutineContext()
                    val result = PianoScaleAnalyzer().analyze(pcm, rate) { context.ensureActive() }
                    val encoded = if (pcm.isNotEmpty() && rate > 0) WavEncoder.encode(pcm, rate, maxSampleRate = 24_000) else null
                    result to encoded
                }
                if (started == generation && foreground) {
                    wav = audio
                    mutableState.update { it.copy(phase = PianoScalePhase.RESULT, report = report) }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (started == generation) mutableState.update { it.copy(phase = PianoScalePhase.READY, message = "Analyse impossible. Recommence une courte prise.") }
            }
        }
    }

    fun listenExample() {
        if (foreground && state.value.allowed && state.value.phase !in listOf(PianoScalePhase.RECORDING, PianoScalePhase.ANALYZING)) {
            preview.stop()
            example.broken(PianoScaleExercise.notes)
        }
    }
    fun listenTake() {
        if (!foreground || state.value.phase != PianoScalePhase.RESULT) return
        wav?.let { preview(PREVIEW_PATH, it, NowPlaying("Ma gamme de Do majeur", "Essai privé sur ce téléphone")) }
    }

    /** Navigation, backgrounding and account changes discard transient audio and pending results. */
    fun interrupt() {
        foreground = false
        generation++
        capture?.cancel()
        analysis?.cancel()
        val interrupted = state.value.phase in listOf(PianoScalePhase.RECORDING, PianoScalePhase.ANALYZING)
        clearAudio()
        mutableState.value = PianoScaleUiState(allowed = state.value.allowed,
            message = if (interrupted) "La prise a été interrompue. Recommence quand tu es prêt." else null)
    }
    private fun clearAudio() {
        example.stop()
        preview.stop()
        files.delete(PREVIEW_PATH)
        chunks.clear()
        sampleRate = 0
        samplesCount = 0
        wav = null
    }
    override fun onCleared() { interrupt() }
    companion object {
        const val MAX_SECONDS = 30
        private const val PREVIEW_PATH = "voice/piano-scale-coach.wav"
    }
}
