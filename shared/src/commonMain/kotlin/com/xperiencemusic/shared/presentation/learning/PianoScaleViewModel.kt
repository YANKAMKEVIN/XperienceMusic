package com.xperiencemusic.shared.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.music.pitch.PianoNoteCorrection
import com.xperiencemusic.music.pitch.PianoScalePractice
import com.xperiencemusic.music.pitch.PianoScaleQuality
import com.xperiencemusic.music.pitch.PianoScaleReport
import com.xperiencemusic.music.pitch.comparePianoScales
import com.xperiencemusic.music.pitch.expectedIndexAt
import com.xperiencemusic.music.pitch.passage
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.repository.NowPlaying
import com.xperiencemusic.shared.domain.usecase.AnalyzePianoScaleUseCase
import com.xperiencemusic.shared.domain.usecase.CapturedFrame
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.PcmRecorder
import com.xperiencemusic.shared.domain.usecase.PianoTake
import com.xperiencemusic.shared.domain.usecase.PlayLearningExampleUseCase
import com.xperiencemusic.shared.domain.usecase.PlayLocalAudioUseCase
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** One bounded local take plus a report baseline; no upload or skill validation. Only the phases live here. */
class PianoScaleViewModel(
    observeMember: ObserveCurrentMemberUseCase,
    private val recorder: PcmRecorder,
    private val example: PlayLearningExampleUseCase,
    private val preview: PlayLocalAudioUseCase,
    private val analyze: AnalyzePianoScaleUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PianoScaleUiState())
    val state = mutableState.asStateFlow()
    val playback get() = preview.state
    private var capture: Job? = null
    private var analysis: Job? = null
    private var stage: Job? = null
    private var replay: Job? = null
    private var take: PianoTake? = null
    private var baseline: PianoScaleReport? = null
    private var signalSeconds = 0.0
    private var recordingPrimed = false
    private var generation = 0
    private var foreground = true
    private var memberId: String? = null

    init {
        observeMember()
            .onEach { member ->
                if (memberId != member?.id || member?.profile?.instrument != Instrument.PIANO) {
                    val active = foreground
                    interrupt()
                    foreground = active
                }
                memberId = member?.id
                mutableState.update { it.copy(allowed = member?.profile?.instrument == Instrument.PIANO) }
            }
            .launchIn(viewModelScope)
    }

    fun activate() {
        foreground = true
    }

    fun denied() {
        if (foreground) mutableState.update { it.copy(message = PianoScaleMessage.MICROPHONE_DENIED) }
    }

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
        if (state.value.report?.steps?.indices?.contains(index) == true && !state.value.busy) {
            mutableState.update { it.copy(selectedStep = index) }
        }
    }

    /** Opens the mic for a disposable signal check; calibration/countdown audio is never stored. */
    fun start() {
        if (!foreground || !state.value.allowed || state.value.busy) return
        state.value.report?.takeIf { it.quality == PianoScaleQuality.USABLE }?.let { baseline = it }
        clearAudio()
        val started = ++generation
        mutableState.value = PianoScaleUiState(allowed = true, phase = PianoScalePhase.PREPARING, practice = state.value.practice)
        stage = viewModelScope.launch {
            delay(PREPARATION_TIMEOUT_MS)
            cancelPreparation(PianoScaleMessage.TEST_TIMED_OUT)
        }
        val previous = capture
        capture = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                previous?.cancelAndJoin()
                recorder.frames(strict = true).collect { frame ->
                    if (started == generation) onFrame(frame)
                }
                if (started == generation) {
                    if (state.value.phase == PianoScalePhase.RECORDING) stop() else cancelPreparation(PianoScaleMessage.MICROPHONE_STOPPED)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (started == generation) failCapture(PianoScaleMessage.MICROPHONE_FAILED)
            }
        }
        capture?.start()
    }

    private fun onFrame(frame: CapturedFrame) {
        val signal = when {
            frame.clippedShare > CLIPPED_SHARE -> PianoInputSignal.LOUD
            frame.loudness >= SOUND_LOUDNESS -> PianoInputSignal.SOUND
            else -> PianoInputSignal.QUIET
        }
        if (signal == PianoInputSignal.SOUND) signalSeconds += frame.samples.size.toDouble() / frame.sampleRate
        mutableState.update { it.copy(level = frame.level, signal = signal, signalSeen = signalSeconds >= SIGNAL_SECONDS) }
        if (state.value.phase != PianoScalePhase.RECORDING) return
        // The first delivered frame can straddle the countdown boundary.
        if (!recordingPrimed) {
            recordingPrimed = true
            return
        }
        val maxSamples = MAX_SECONDS * frame.sampleRate
        recorder.keep(frame, maxSamples)
        mutableState.update { it.copy(seconds = recorder.seconds) }
        if (recorder.sampleCount >= maxSamples) stop()
    }

    fun beginCountdown() {
        val s = state.value
        if (!foreground || s.phase != PianoScalePhase.PREPARING || !s.signalSeen || s.signal == PianoInputSignal.LOUD) return
        stage?.cancel()
        mutableState.update { it.copy(phase = PianoScalePhase.COUNTDOWN, countdown = COUNTDOWN, message = null) }
        stage = viewModelScope.launch {
            for (count in COUNTDOWN downTo 1) {
                mutableState.update { it.copy(countdown = count) }
                delay(1_000)
            }
            mutableState.update { it.copy(phase = PianoScalePhase.RECORDING, seconds = 0.0) }
            delay(MAX_SECONDS * 1_000L)
            stop()
        }
    }

    fun cancelPreparation(message: PianoScaleMessage? = null) {
        if (state.value.phase !in listOf(PianoScalePhase.PREPARING, PianoScalePhase.COUNTDOWN)) return
        failCapture(message)
    }

    private fun failCapture(message: PianoScaleMessage?) {
        generation++
        capture?.cancel()
        stage?.cancel()
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
                val samples = recorder.samples()
                val rate = recorder.sampleRate
                recorder.clear()
                val result = analyze(samples, rate, targets)
                if (started == generation && foreground) {
                    take = result
                    val report = result.report
                    val selected = report.steps.indexOfFirst { it.correction != PianoNoteCorrection.MATCH }.coerceAtLeast(0)
                    mutableState.update {
                        it.copy(
                            phase = PianoScalePhase.RESULT,
                            report = report,
                            comparison = comparePianoScales(baseline, report),
                            selectedStep = selected.takeIf { report.steps.isNotEmpty() },
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (started == generation) {
                    mutableState.update { it.copy(phase = PianoScalePhase.READY, message = PianoScaleMessage.ANALYSIS_FAILED) }
                }
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

    /** Plays the whole take back; the screen names it for the system controls. */
    fun listenTake(nowPlaying: NowPlaying) {
        if (!foreground || state.value.phase != PianoScalePhase.RESULT) return
        stopPlayback()
        take?.wav?.let { preview(PREVIEW_PATH, it, nowPlaying) }
    }

    fun listenPassage(index: Int, nowPlaying: NowPlaying) {
        val s = state.value
        val current = take ?: return
        if (!foreground || s.phase != PianoScalePhase.RESULT || s.preparingReplay || current.sampleRate <= 0) return
        val range = s.report?.passage(index, current.seconds) ?: return
        stopPlayback()
        val started = generation
        mutableState.update { it.copy(preparingReplay = true, selectedStep = index) }
        replay = viewModelScope.launch {
            try {
                val audio = analyze.passage(current, range)
                if (started == generation && foreground) preview(PASSAGE_PATH, audio, nowPlaying)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (started == generation) mutableState.update { it.copy(message = PianoScaleMessage.PASSAGE_FAILED) }
            } finally {
                if (started == generation) mutableState.update { it.copy(preparingReplay = false) }
            }
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
        capture?.cancel()
        analysis?.cancel()
        stage?.cancel()
        val interrupted = state.value.busy
        clearAudio()
        baseline = null
        mutableState.value = PianoScaleUiState(
            allowed = state.value.allowed,
            practice = state.value.practice,
            message = if (interrupted) PianoScaleMessage.INTERRUPTED else null,
        )
    }

    private fun clearAudio() {
        stopPlayback()
        preview.discard(PREVIEW_PATH, PASSAGE_PATH)
        recorder.clear()
        signalSeconds = 0.0
        take = null
        recordingPrimed = false
    }

    override fun onCleared() = interrupt()

    companion object {
        const val MAX_SECONDS = 30
        private const val COUNTDOWN = 3
        private const val PREPARATION_TIMEOUT_MS = 20_000L

        /** Share of full-scale samples above which the sound saturates. */
        private const val CLIPPED_SHARE = 0.02

        /** Loudness (RMS) from which the microphone hears the piano. */
        private const val SOUND_LOUDNESS = 0.008

        /** Seconds of sound needed before the countdown can start. */
        private const val SIGNAL_SECONDS = 0.15
        private const val PREVIEW_PATH = "voice/piano-scale-coach.wav"
        private const val PASSAGE_PATH = "voice/piano-scale-passage.wav"
    }
}
