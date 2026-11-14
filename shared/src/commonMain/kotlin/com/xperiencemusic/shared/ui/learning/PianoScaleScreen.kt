package com.xperiencemusic.shared.ui.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.music.pitch.PianoScalePractice
import com.xperiencemusic.shared.domain.repository.NowPlaying
import com.xperiencemusic.shared.presentation.learning.PianoInputSignal
import com.xperiencemusic.shared.presentation.learning.PianoScalePhase
import com.xperiencemusic.shared.presentation.learning.PianoScaleUiState
import com.xperiencemusic.shared.presentation.learning.PianoScaleViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.piano_scale_analyzing
import com.xperiencemusic.shared.resources.piano_scale_beat_note
import com.xperiencemusic.shared.resources.piano_scale_cancel_check
import com.xperiencemusic.shared.resources.piano_scale_cancel_start
import com.xperiencemusic.shared.resources.piano_scale_check_text
import com.xperiencemusic.shared.resources.piano_scale_check_title
import com.xperiencemusic.shared.resources.piano_scale_countdown_text
import com.xperiencemusic.shared.resources.piano_scale_countdown_title
import com.xperiencemusic.shared.resources.piano_scale_direction
import com.xperiencemusic.shared.resources.piano_scale_example
import com.xperiencemusic.shared.resources.piano_scale_eyebrow_focus
import com.xperiencemusic.shared.resources.piano_scale_eyebrow_scale
import com.xperiencemusic.shared.resources.piano_scale_follow
import com.xperiencemusic.shared.resources.piano_scale_go
import com.xperiencemusic.shared.resources.piano_scale_instructions
import com.xperiencemusic.shared.resources.piano_scale_listen_take
import com.xperiencemusic.shared.resources.piano_scale_not_allowed
import com.xperiencemusic.shared.resources.piano_scale_passage_subtitle
import com.xperiencemusic.shared.resources.piano_scale_passage_title
import com.xperiencemusic.shared.resources.piano_scale_prepare
import com.xperiencemusic.shared.resources.piano_scale_recording
import com.xperiencemusic.shared.resources.piano_scale_redo
import com.xperiencemusic.shared.resources.piano_scale_signal_loud
import com.xperiencemusic.shared.resources.piano_scale_signal_seen
import com.xperiencemusic.shared.resources.piano_scale_signal_waiting
import com.xperiencemusic.shared.resources.piano_scale_stop
import com.xperiencemusic.shared.resources.piano_scale_stop_after
import com.xperiencemusic.shared.resources.piano_scale_stop_audio
import com.xperiencemusic.shared.resources.piano_scale_take_subtitle
import com.xperiencemusic.shared.resources.piano_scale_take_title
import com.xperiencemusic.shared.resources.piano_scale_title
import com.xperiencemusic.shared.resources.piano_scale_your_turn
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.MicrophoneDeniedNotice
import com.xperiencemusic.shared.ui.platform.KeepScreenOn
import com.xperiencemusic.shared.ui.platform.MicrophoneUse
import com.xperiencemusic.shared.ui.platform.rememberMicrophonePermission
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PianoScaleScreen(onBack: () -> Unit, vm: PianoScaleViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val permission = rememberMicrophonePermission(MicrophoneUse.LISTEN_PIANO)
    if (state.phase in listOf(PianoScalePhase.PREPARING, PianoScalePhase.COUNTDOWN, PianoScalePhase.RECORDING)) KeepScreenOn()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.activate() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.interrupt() }
    DisposableEffect(vm) { onDispose { vm.interrupt() } }
    val take = NowPlaying(stringResource(Res.string.piano_scale_take_title), stringResource(Res.string.piano_scale_take_subtitle))
    val passage = NowPlaying(stringResource(Res.string.piano_scale_passage_title), stringResource(Res.string.piano_scale_passage_subtitle))
    PianoScaleScreenContent(
        state,
        playback.isPlaying,
        onBack = {
            vm.interrupt()
            onBack()
        },
        actions = PianoScaleActions(
            start = { permission.request { if (it) vm.start() else vm.denied() } },
            stop = vm::stop,
            countdown = vm::beginCountdown,
            cancel = { vm.cancelPreparation() },
            example = vm::listenExample,
            listen = { vm.listenTake(take) },
            stopAudio = vm::stopPlayback,
            configure = vm::configure,
            select = vm::selectStep,
            passage = { vm.listenPassage(it, passage) },
            expected = vm::listenExpected,
            focus = vm::focusStep,
            openSettings = permission.openSettings,
        ),
    )
}

class PianoScaleActions(
    val start: () -> Unit = {},
    val stop: () -> Unit = {},
    val countdown: () -> Unit = {},
    val cancel: () -> Unit = {},
    val example: () -> Unit = {},
    val listen: () -> Unit = {},
    val stopAudio: () -> Unit = {},
    val configure: (PianoScalePractice) -> Unit = {},
    val select: (Int) -> Unit = {},
    val passage: (Int) -> Unit = {},
    val expected: (Int) -> Unit = {},
    val focus: (Int) -> Unit = {},
    val openSettings: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PianoScaleScreenContent(
    state: PianoScaleUiState,
    playing: Boolean = false,
    onBack: () -> Unit,
    actions: PianoScaleActions = PianoScaleActions(),
) {
    // The report appears below the settings that come back after the take: scroll down to it
    // instead of leaving the page at the top.
    val reportRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(state.report) {
        if (state.report != null) {
            withFrameNanos { }
            reportRequester.bringIntoView()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.piano_scale_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.action_back)) }
                },
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            ExerciseCard(state.practice)
            if (!state.allowed) {
                Text(stringResource(Res.string.piano_scale_not_allowed))
            } else {
                if (!state.busy) PracticeSettings(state, actions)
                if (state.report == null || state.busy) TargetNotes(state)
                when (state.phase) {
                    PianoScalePhase.PREPARING -> MicrophoneCheck(state, actions)
                    PianoScalePhase.COUNTDOWN -> Countdown(state, actions)
                    PianoScalePhase.RECORDING -> RecordingCard(state, actions)
                    PianoScalePhase.ANALYZING -> {
                        CircularProgressIndicator()
                        Text(stringResource(Res.string.piano_scale_analyzing), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    PianoScalePhase.READY, PianoScalePhase.RESULT -> {
                        state.report?.let { Box(Modifier.bringIntoViewRequester(reportRequester)) { ScaleReport(state, it, actions) } }
                        ReadyActions(state, actions)
                    }
                }
            }
            if (playing) OutlinedButton(onClick = actions.stopAudio) { Text(stringResource(Res.string.piano_scale_stop_audio)) }
            state.message?.let { message ->
                if (state.microphoneDenied) {
                    MicrophoneDeniedNotice(message.text(), actions.openSettings)
                } else {
                    Text(message.text(), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            GoodToKnow()
        }
    }
}

@Composable
private fun ExerciseCard(practice: PianoScalePractice) {
    AppCard {
        Text(
            stringResource(if (practice.focusStart == null) Res.string.piano_scale_eyebrow_scale else Res.string.piano_scale_eyebrow_focus),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(practice.key.label(), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(Res.string.piano_scale_direction, directionLabel(practice.descending), practice.bpm),
            Modifier.padding(top = Spacing.s),
        )
        Text(stringResource(Res.string.piano_scale_instructions), Modifier.padding(top = Spacing.s))
    }
}

@Composable
private fun MicrophoneCheck(state: PianoScaleUiState, actions: PianoScaleActions) {
    AppCard {
        Text(stringResource(Res.string.piano_scale_check_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(Res.string.piano_scale_check_text), Modifier.padding(top = Spacing.s))
        LinearProgressIndicator(progress = { state.level }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.m))
        Text(
            stringResource(
                when {
                    state.signal == PianoInputSignal.LOUD -> Res.string.piano_scale_signal_loud
                    state.signalSeen -> Res.string.piano_scale_signal_seen
                    else -> Res.string.piano_scale_signal_waiting
                },
            ),
            Modifier.padding(top = Spacing.s).semantics { liveRegion = LiveRegionMode.Polite },
        )
        GradientButton(
            onClick = actions.countdown,
            enabled = state.signalSeen && state.signal != PianoInputSignal.LOUD,
            modifier = Modifier.padding(top = Spacing.m),
        ) {
            Text(stringResource(Res.string.piano_scale_go))
        }
        TextButton(onClick = actions.cancel) { Text(stringResource(Res.string.piano_scale_cancel_check)) }
    }
}

@Composable
private fun Countdown(state: PianoScaleUiState, actions: PianoScaleActions) {
    AppCard {
        Text(stringResource(Res.string.piano_scale_countdown_title), style = MaterialTheme.typography.titleLarge)
        Text(
            "${state.countdown}",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(vertical = Spacing.l).semantics { liveRegion = LiveRegionMode.Assertive },
        )
        Text(stringResource(Res.string.piano_scale_countdown_text))
        TextButton(onClick = actions.cancel) { Text(stringResource(Res.string.piano_scale_cancel_start)) }
    }
}

@Composable
private fun RecordingCard(state: PianoScaleUiState, actions: PianoScaleActions) {
    AppCard {
        Text(
            stringResource(Res.string.piano_scale_your_turn),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(stringResource(Res.string.piano_scale_recording, state.seconds.toInt(), PianoScaleViewModel.MAX_SECONDS, state.practice.bpm))
        LinearProgressIndicator(progress = { state.level }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.m))
        Text(
            stringResource(if (state.visualBeat == null) Res.string.piano_scale_stop_after else Res.string.piano_scale_follow),
            Modifier.padding(top = Spacing.s),
        )
        Text(stringResource(Res.string.piano_scale_beat_note), style = MaterialTheme.typography.bodySmall)
        GradientButton(onClick = actions.stop, modifier = Modifier.padding(top = Spacing.m)) {
            Text(stringResource(Res.string.piano_scale_stop))
        }
    }
}

@Composable
private fun ReadyActions(state: PianoScaleUiState, actions: PianoScaleActions) {
    OutlinedButton(onClick = actions.example) { Text(stringResource(Res.string.piano_scale_example, state.practice.bpm)) }
    if (state.phase == PianoScalePhase.RESULT && state.seconds > 0) {
        OutlinedButton(onClick = actions.listen) { Text(stringResource(Res.string.piano_scale_listen_take)) }
    }
    GradientButton(onClick = actions.start) {
        Text(stringResource(if (state.phase == PianoScalePhase.RESULT) Res.string.piano_scale_redo else Res.string.piano_scale_prepare))
    }
}

@Preview
@Composable
private fun PianoWorkshopPreview() = AppTheme {
    PianoScaleScreenContent(PianoScaleUiState(allowed = true, phase = PianoScalePhase.PREPARING, signalSeen = true), onBack = {})
}
