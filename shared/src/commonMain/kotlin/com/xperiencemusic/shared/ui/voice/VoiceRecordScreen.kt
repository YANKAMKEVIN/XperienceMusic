package com.xperiencemusic.shared.ui.voice

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.voice.GuideKind
import com.xperiencemusic.shared.presentation.voice.GuideUi
import com.xperiencemusic.shared.presentation.voice.PreviousSubmissionUi
import com.xperiencemusic.shared.presentation.voice.VoiceError
import com.xperiencemusic.shared.presentation.voice.VoicePhase
import com.xperiencemusic.shared.presentation.voice.VoiceRecordEvent
import com.xperiencemusic.shared.presentation.voice.VoiceRecordUiState
import com.xperiencemusic.shared.presentation.voice.VoiceRecordViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.voice_earphones
import com.xperiencemusic.shared.resources.voice_error_mic
import com.xperiencemusic.shared.resources.voice_error_send
import com.xperiencemusic.shared.resources.voice_error_short
import com.xperiencemusic.shared.resources.voice_guide_none
import com.xperiencemusic.shared.resources.voice_guide_reference
import com.xperiencemusic.shared.resources.voice_guide_title
import com.xperiencemusic.shared.resources.voice_guide_voice
import com.xperiencemusic.shared.resources.voice_listen
import com.xperiencemusic.shared.resources.voice_pause
import com.xperiencemusic.shared.resources.voice_previous
import com.xperiencemusic.shared.resources.voice_record
import com.xperiencemusic.shared.resources.voice_recording
import com.xperiencemusic.shared.resources.voice_retake
import com.xperiencemusic.shared.resources.voice_send
import com.xperiencemusic.shared.resources.voice_sent_text
import com.xperiencemusic.shared.resources.voice_sent_title
import com.xperiencemusic.shared.resources.voice_stop
import com.xperiencemusic.shared.resources.voice_subtitle
import com.xperiencemusic.shared.resources.voice_title
import com.xperiencemusic.shared.ui.brand.BrandWaves
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.brand.KeyPill
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.PauseIcon
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.platform.rememberMicrophonePermission
import com.xperiencemusic.shared.ui.progress.ExerciseStatusBadge
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun VoiceRecordScreen(
    songId: String,
    onBack: () -> Unit,
    viewModel: VoiceRecordViewModel = koinViewModel(key = "voice-$songId") { parametersOf(songId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val microphone = rememberMicrophonePermission()
    VoiceRecordScreenContent(
        state = state,
        onEvent = { event ->
            if (event == VoiceRecordEvent.RecordClicked) {
                microphone.request { granted ->
                    viewModel.onEvent(if (granted) VoiceRecordEvent.RecordClicked else VoiceRecordEvent.PermissionDenied)
                }
            } else {
                viewModel.onEvent(event)
            }
        },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecordScreenContent(state: VoiceRecordUiState, onEvent: (VoiceRecordEvent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.voice_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SongHeader(state)
            state.previous?.let { PreviousCard(it) }
            if (state.phase == VoicePhase.SENT) {
                SentCard()
            } else {
                if (state.phase == VoicePhase.READY && state.guides.isNotEmpty()) GuidePicker(state, onEvent)
                Recorder(state, onEvent)
            }
            state.error?.let { error ->
                Text(
                    stringResource(
                        when (error) {
                            VoiceError.MICROPHONE -> Res.string.voice_error_mic
                            VoiceError.TOO_SHORT -> Res.string.voice_error_short
                            VoiceError.SEND_FAILED -> Res.string.voice_error_send
                        },
                    ),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@Composable
private fun SongHeader(state: VoiceRecordUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(state.songTitle, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.s)) {
            KeyPill(state.keyLabel)
            state.voicePart?.let {
                Text(
                    stringResource(Res.string.voice_subtitle, it.label()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.s),
                )
            }
        }
    }
}

@Composable
private fun PreviousCard(previous: PreviousSubmissionUi) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.voice_previous, previous.date),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            ExerciseStatusBadge(previous.status)
        }
        previous.feedback?.let {
            Text("« $it »", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.s))
        }
    }
}

@Composable
private fun GuidePicker(state: VoiceRecordUiState, onEvent: (VoiceRecordEvent) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.voice_guide_title), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(top = Spacing.s)) {
            state.guides.forEach { guide -> GuideChip(guideLabel(guide), state.selectedGuideId == guide.id) { onEvent(VoiceRecordEvent.GuideSelected(guide.id)) } }
            GuideChip(stringResource(Res.string.voice_guide_none), state.selectedGuideId == null) { onEvent(VoiceRecordEvent.GuideSelected(null)) }
        }
        Text(
            stringResource(Res.string.voice_earphones),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.s),
        )
    }
}

@Composable
private fun guideLabel(guide: GuideUi): String = when (guide.kind) {
    GuideKind.MY_VOICE -> stringResource(Res.string.voice_guide_voice, guide.voicePart?.label().orEmpty())
    GuideKind.REFERENCE -> stringResource(Res.string.voice_guide_reference)
}

@Composable
private fun GuideChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

/** The big round button, the time, and the waves that follow the voice. */
@Composable
private fun Recorder(state: VoiceRecordUiState, onEvent: (VoiceRecordEvent) -> Unit) {
    val isRecording = state.phase == VoicePhase.RECORDING
    val pulse by animateFloatAsState(1f + state.level * 0.35f, label = "voiceLevel")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            formatTime(state.seconds),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            stringResource(if (isRecording) Res.string.voice_recording else Res.string.voice_record),
            style = MaterialTheme.typography.labelLarge,
            color = if (isRecording) BrandColors.Purple else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BrandWaves(Modifier.fillMaxWidth().height(80.dp).scale(scaleX = 1f, scaleY = if (isRecording) pulse else 0.4f))
        when (state.phase) {
            VoicePhase.READY, VoicePhase.RECORDING -> {
                val label = stringResource(if (isRecording) Res.string.voice_stop else Res.string.voice_record)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = Spacing.s)) {
                    if (isRecording) {
                        Box(
                            Modifier.size(96.dp).scale(pulse)
                                .background(Brush.radialGradient(listOf(BrandColors.Purple.copy(alpha = 0.4f), Color.Transparent)), CircleShape),
                        )
                    }
                    Box(
                        Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(BrandColors.gradient)
                            .clickable(onClickLabel = label, role = Role.Button) {
                                onEvent(if (isRecording) VoiceRecordEvent.StopClicked else VoiceRecordEvent.RecordClicked)
                            }
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isRecording) {
                            Box(Modifier.size(26.dp).background(Color.White, RoundedCornerShape(6.dp)))
                        } else {
                            Icon(XmIcons.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }
            VoicePhase.RECORDED, VoicePhase.SENDING -> {
                val sending = state.phase == VoicePhase.SENDING
                OutlinedButton(onClick = { onEvent(VoiceRecordEvent.PreviewClicked) }, enabled = !sending) {
                    Icon(if (state.isPreviewPlaying) PauseIcon else Icons.Filled.PlayArrow, contentDescription = null)
                    Text(
                        stringResource(if (state.isPreviewPlaying) Res.string.voice_pause else Res.string.voice_listen),
                        modifier = Modifier.padding(start = Spacing.s),
                    )
                }
                GradientButton(
                    onClick = { onEvent(VoiceRecordEvent.SendClicked) },
                    enabled = !sending,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.l),
                ) {
                    if (sending) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Text(stringResource(Res.string.voice_send), style = MaterialTheme.typography.titleMedium)
                    }
                }
                TextButton(onClick = { onEvent(VoiceRecordEvent.RetakeClicked) }, enabled = !sending) {
                    Text(stringResource(Res.string.voice_retake))
                }
            }
            VoicePhase.SENT -> Unit
        }
    }
}

@Composable
private fun SentCard() {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Text(
                stringResource(Res.string.voice_sent_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.s),
            )
        }
        Text(
            stringResource(Res.string.voice_sent_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.s),
        )
    }
}

/** "1:05" */
private fun formatTime(seconds: Int) = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

@Preview
@Composable
private fun VoiceRecordPreview() {
    AppTheme {
        VoiceRecordScreenContent(
            VoiceRecordUiState(
                isLoading = false,
                songTitle = "À toi la gloire",
                keyLabel = "Ré",
                voicePart = VoicePart.ALTO,
                guides = listOf(GuideUi("t", "ALTO", GuideKind.MY_VOICE, VoicePart.ALTO), GuideUi("r", "Répétition", GuideKind.REFERENCE)),
                selectedGuideId = "t",
                previous = PreviousSubmissionUi("lundi 12 oct.", ExerciseStatus.TO_REWORK, "Attention à la justesse au refrain."),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
