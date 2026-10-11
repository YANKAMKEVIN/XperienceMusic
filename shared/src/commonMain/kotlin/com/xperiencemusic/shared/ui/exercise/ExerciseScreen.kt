package com.xperiencemusic.shared.ui.exercise

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
                actions = { ExerciseStatusBadge(state.status, Modifier.padding(end = Spacing.l)) },
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
                AutomaticCheck(check, state.phase, onEvent)
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
private fun AutomaticCheck(check: AutomaticCheckUi, phase: ExercisePhase, onEvent: (ExerciseEvent) -> Unit) {
    AppCard {
        Text(
            stringResource(Res.string.exercise_target, check.targetNoteLabel, check.requiredSeconds),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(onClick = { onEvent(ExerciseEvent.PlayReferenceClicked) }, modifier = Modifier.padding(top = Spacing.s)) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = Spacing.s))
            Text(stringResource(Res.string.exercise_listen_reference))
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
            Text(stringResource(Res.string.exercise_success_text), modifier = Modifier.padding(top = Spacing.s))
            OutlinedButton(onClick = { onEvent(ExerciseEvent.StartClicked) }, modifier = Modifier.padding(top = Spacing.m)) {
                Text(stringResource(Res.string.exercise_retry))
            }
        }
        ExercisePhase.LISTENING -> Live(check, onEvent)
        ExercisePhase.READY -> {
            Button(onClick = { onEvent(ExerciseEvent.StartClicked) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.exercise_start))
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
        Text(
            check.heardNoteLabel?.let { stringResource(Res.string.exercise_heard, it) } ?: "",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        TunerNeedle(check.needle, description = stringResource(Res.string.exercise_tuner))
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
        LinearProgressIndicator(progress = { check.holdProgress }, modifier = Modifier.fillMaxWidth())
        Text(
            stringResource(Res.string.exercise_hold, formatSeconds(check.holdSeconds), check.requiredSeconds),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { onEvent(ExerciseEvent.StopClicked) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.exercise_stop))
        }
    }
}

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
