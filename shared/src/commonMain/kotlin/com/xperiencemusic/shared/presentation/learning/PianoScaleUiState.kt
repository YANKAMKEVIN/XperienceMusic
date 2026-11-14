package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.music.pitch.PianoScaleComparison
import com.xperiencemusic.music.pitch.PianoScalePractice
import com.xperiencemusic.music.pitch.PianoScaleReport

enum class PianoScalePhase { READY, PREPARING, COUNTDOWN, RECORDING, ANALYZING, RESULT }

enum class PianoInputSignal { QUIET, SOUND, LOUD }

/** What the workshop says after something stopped or failed. */
enum class PianoScaleMessage {
    /** The permission was refused: the screen offers the phone settings. */
    MICROPHONE_DENIED,
    TEST_TIMED_OUT,
    MICROPHONE_STOPPED,
    MICROPHONE_FAILED,
    ANALYSIS_FAILED,
    PASSAGE_FAILED,
    INTERRUPTED,
}

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
    val message: PianoScaleMessage? = null,
) {
    /** The microphone was refused: the screen offers the phone settings. */
    val microphoneDenied get() = message == PianoScaleMessage.MICROPHONE_DENIED

    val busy get() = phase !in listOf(PianoScalePhase.READY, PianoScalePhase.RESULT)

    val visualBeat get() = (seconds * practice.bpm / 60).toInt().takeIf { it in practice.notes.indices }
}
