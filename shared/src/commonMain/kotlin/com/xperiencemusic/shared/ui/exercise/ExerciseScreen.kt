package com.xperiencemusic.shared.ui.exercise

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.xperiencemusic.shared.resources.exercise_accompaniment
import com.xperiencemusic.shared.resources.exercise_locked
import com.xperiencemusic.shared.resources.exercise_step
import com.xperiencemusic.shared.resources.exercise_success_challenge
import com.xperiencemusic.shared.resources.exercise_success_warmup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.resources.exercise_mic_on
import com.xperiencemusic.shared.resources.exercise_target_label
import com.xperiencemusic.shared.ui.brand.BrandWaves
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.theme.BrandColors
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.presentation.exercise.AutomaticCheckUi
import com.xperiencemusic.shared.presentation.exercise.ExerciseError
import com.xperiencemusic.shared.presentation.exercise.ExerciseEvent
import com.xperiencemusic.shared.presentation.exercise.ExercisePhase
import com.xperiencemusic.shared.presentation.exercise.ExerciseUiState
import com.xperiencemusic.shared.presentation.exercise.ExerciseViewModel
import com.xperiencemusic.shared.presentation.exercise.Tuning
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.exercise_error_denied
import com.xperiencemusic.shared.resources.exercise_error_mic
import com.xperiencemusic.shared.resources.exercise_error_save
import com.xperiencemusic.shared.resources.exercise_heard
import com.xperiencemusic.shared.resources.exercise_hold
import com.xperiencemusic.shared.resources.exercise_in_tune
import com.xperiencemusic.shared.resources.exercise_listen_reference
import com.xperiencemusic.shared.resources.exercise_privacy
import com.xperiencemusic.shared.resources.exercise_retry
import com.xperiencemusic.shared.resources.exercise_review_text
import com.xperiencemusic.shared.resources.exercise_silent
import com.xperiencemusic.shared.resources.exercise_start
import com.xperiencemusic.shared.resources.exercise_stop
import com.xperiencemusic.shared.resources.exercise_success_text
import com.xperiencemusic.shared.resources.exercise_success_title
import com.xperiencemusic.shared.resources.exercise_target
import com.xperiencemusic.shared.resources.exercise_too_high
import com.xperiencemusic.shared.resources.exercise_too_low
import com.xperiencemusic.shared.resources.exercise_tuner
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.platform.rememberMicrophonePermission
import com.xperiencemusic.shared.ui.progress.ExerciseStatusBadge
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ExerciseScreen(
    exerciseId: String,
    onBack: () -> Unit,
    viewModel: ExerciseViewModel = koinViewModel(key = "exercise-$exerciseId") { parametersOf(exerciseId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val microphone = rememberMicrophonePermission()
    ExerciseScreenContent(
        state = state,
        onEvent = { event ->
            if (event == ExerciseEvent.StartClicked) {
                microphone.request { granted ->
                    viewModel.onEvent(if (granted) ExerciseEvent.StartClicked else ExerciseEvent.PermissionDenied)
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
fun ExerciseScreenContent(state: ExerciseUiState, onEvent: (ExerciseEvent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                actions = { if (!state.isWarmUp) ExerciseStatusBadge(state.status, Modifier.padding(end = Spacing.l)) },
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
        ) {
            Text(state.instructions, style = MaterialTheme.typography.bodyLarge)
            val check = state.check
            if (check == null) {
                AppCard { Text(stringResource(Res.string.exercise_review_text), style = MaterialTheme.typography.bodyMedium) }
            } else {
                AutomaticCheck(check, state, onEvent)
            }
            state.error?.let { error ->
                Text(
                    stringResource(
                        when (error) {
                            ExerciseError.MICROPHONE_DENIED -> Res.string.exercise_error_denied
                            ExerciseError.MICROPHONE_UNAVAILABLE -> Res.string.exercise_error_mic
                            ExerciseError.SAVE_FAILED -> Res.string.exercise_error_save
                        },
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AutomaticCheck(check: AutomaticCheckUi, state: ExerciseUiState, onEvent: (ExerciseEvent) -> Unit) {
    val phase = state.phase
    AppCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(Res.string.exercise_target_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                check.targetNoteLabel,
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.semantics {
                    contentDescription = "${check.targetNoteLabel}, ${check.requiredSeconds} s"
                },
            )
            Text(
                "${frequencyOf(check.targetMidi)} Hz · ${check.requiredSeconds} s",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (check.isSequence) NoteSteps(check, isListening = phase == ExercisePhase.LISTENING)
            check.accompanimentLabel?.let {
                Text(
                    stringResource(Res.string.exercise_accompaniment, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.s),
                )
            }
            if (phase == ExercisePhase.LISTENING) {
                TunerNeedle(check.needle, description = stringResource(Res.string.exercise_tuner), modifier = Modifier.padding(top = Spacing.l))
                Row(Modifier.fillMaxWidth().padding(top = Spacing.xs)) {
                    listOf("−50", "0", "+50").forEachIndexed { index, label ->
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = when (index) {
                                0 -> TextAlign.Start
                                1 -> TextAlign.Center
                                else -> TextAlign.End
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            TextButton(onClick = { onEvent(ExerciseEvent.PlayReferenceClicked) }, modifier = Modifier.padding(top = Spacing.s)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = Spacing.s))
                Text(stringResource(Res.string.exercise_listen_reference))
            }
        }
    }

    when (phase) {
        ExercisePhase.SUCCEEDED -> AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Text(
                    stringResource(Res.string.exercise_success_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = Spacing.s),
                )
            }
            Text(
                stringResource(
                    when {
                        state.isWarmUp -> Res.string.exercise_success_warmup
                        state.isChallenge -> Res.string.exercise_success_challenge
                        else -> Res.string.exercise_success_text
                    },
                ),
                modifier = Modifier.padding(top = Spacing.s),
            )
            OutlinedButton(onClick = { onEvent(ExerciseEvent.StartClicked) }, modifier = Modifier.padding(top = Spacing.m)) {
                Text(stringResource(Res.string.exercise_retry))
            }
        }
        ExercisePhase.LISTENING -> Live(check, onEvent)
        ExercisePhase.READY -> if (state.isLocked) {
            Text(
                stringResource(Res.string.exercise_locked),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            GradientButton(onClick = { onEvent(ExerciseEvent.StartClicked) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.exercise_start), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                stringResource(Res.string.exercise_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Live(check: AutomaticCheckUi, onEvent: (ExerciseEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        // The waves say the microphone hears something; the gauge above gives the measure.
        BrandWaves(Modifier.fillMaxWidth().height(72.dp).alpha(if (check.tuning == Tuning.SILENT) 0.35f else 1f))
        Text(
            check.heardNoteLabel?.let { stringResource(Res.string.exercise_heard, it) } ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(
                when (check.tuning) {
                    Tuning.SILENT -> Res.string.exercise_silent
                    Tuning.TOO_LOW -> Res.string.exercise_too_low
                    Tuning.IN_TUNE -> Res.string.exercise_in_tune
                    Tuning.TOO_HIGH -> Res.string.exercise_too_high
                },
            ),
            style = MaterialTheme.typography.titleMedium,
            color = if (check.tuning == Tuning.IN_TUNE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            stringResource(Res.string.exercise_hold, formatSeconds(check.holdSeconds), check.requiredSeconds),
            style = MaterialTheme.typography.titleLarge,
        )
        Box(
            Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                Modifier.fillMaxWidth(check.holdProgress).fillMaxHeight().clip(CircleShape).background(BrandColors.horizontalGradient),
            )
        }
        GradientButton(onClick = { onEvent(ExerciseEvent.StopClicked) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.exercise_stop), style = MaterialTheme.typography.titleMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
            Text(
                stringResource(Res.string.exercise_mic_on),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(start = Spacing.s),
            )
        }
    }
}

/** The notes of a sequence: done ones in the gradient, the current one outlined. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteSteps(check: AutomaticCheckUi, isListening: Boolean) {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.m),
    ) {
        check.noteLabels.forEachIndexed { index, label ->
            val isDone = isListening && index < check.currentStep
            val isCurrent = isListening && index == check.currentStep
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    isDone -> Color.White
                    isCurrent -> colors.onSurface
                    else -> colors.onSurfaceVariant
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isDone) BrandColors.gradient else SolidColor(colors.surfaceContainerHigh))
                    .border(if (isCurrent) 2.dp else 0.dp, if (isCurrent) colors.primary else Color.Transparent, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
    if (isListening) {
        Text(
            stringResource(Res.string.exercise_step, check.currentStep + 1, check.noteLabels.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

/** Equal temperament, A4 = 440 Hz: "220". */
private fun frequencyOf(midi: Int): Int = (440.0 * 2.0.pow((midi - 69) / 12.0)).roundToInt()

/** "2,4" — one decimal, French separator. */
private fun formatSeconds(seconds: Double): String {
    val tenths = (seconds * 10).toInt()
    return "${tenths / 10},${tenths % 10}"
}

@Preview
@Composable
private fun ExerciseScreenListeningPreview() {
    AppTheme {
        ExerciseScreenContent(
            ExerciseUiState(
                isLoading = false,
                title = "Monter à la quinte",
                instructions = "Même exercice, cinq notes au-dessus de ta note de confort.",
                status = ExerciseStatus.TODO,
                check = AutomaticCheckUi("La4", 69, 5, heardNoteLabel = "La4", needle = 0.2f, tuning = Tuning.IN_TUNE, holdSeconds = 2.4),
                phase = ExercisePhase.LISTENING,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun ExerciseScreenSucceededPreview() {
    AppTheme {
        ExerciseScreenContent(
            ExerciseUiState(
                isLoading = false,
                title = "Tenir ta note",
                instructions = "Chante ta note de confort sur « a ».",
                status = ExerciseStatus.VALIDATED,
                check = AutomaticCheckUi("Ré4", 62, 5, holdSeconds = 5.0),
                phase = ExercisePhase.SUCCEEDED,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
