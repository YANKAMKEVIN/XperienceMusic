package com.xperiencemusic.shared.ui.learning

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.presentation.learning.LearningEvent
import com.xperiencemusic.shared.presentation.learning.LearningMode
import com.xperiencemusic.shared.presentation.learning.LearningUiState
import com.xperiencemusic.shared.presentation.learning.LearningViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.learning_comfort
import com.xperiencemusic.shared.resources.learning_not_for_piano
import com.xperiencemusic.shared.resources.learning_not_for_singer
import com.xperiencemusic.shared.resources.learning_pause
import com.xperiencemusic.shared.resources.learning_show_steps
import com.xperiencemusic.shared.resources.learning_step_of
import com.xperiencemusic.shared.ui.components.LoadingIndicator
import com.xperiencemusic.shared.ui.components.SystemBackHandler
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Where the guided path leads out of itself. */
class LearningNavigation(
    val onBack: () -> Unit,
    val onModule: (String) -> Unit = {},
    val onRecord: () -> Unit = {},
    val onScaleCoach: () -> Unit = {},
)

@Composable
fun LearningScreen(
    onBack: () -> Unit,
    moduleId: String = LearningCatalog.DEFAULT,
    onModule: (String) -> Unit = {},
    onRecord: () -> Unit = {},
    onScaleCoach: () -> Unit = {},
    viewModel: LearningViewModel = koinViewModel(key = "learning-$moduleId") { parametersOf(moduleId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LearningScreenContent(state, viewModel::onEvent, LearningNavigation(onBack, onModule, onRecord, onScaleCoach))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreenContent(state: LearningUiState, onEvent: (LearningEvent) -> Unit, navigation: LearningNavigation) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state.successSerial) {
        if (state.successSerial > 0 && state.preferences.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    // The system back gesture does what the arrow does: from an activity or the summary, back to the module.
    SystemBackHandler(enabled = state.mode != LearningMode.HOME) { onEvent(LearningEvent.Home) }
    val fontScale = density.fontScale * if (state.preferences.largeText) LARGE_TEXT_SCALE else 1f
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        Scaffold(
            topBar = { LearningTopBar(state, onEvent, navigation.onBack) },
            // The main action stays on screen however long the hints and corrections above it grow.
            bottomBar = {
                if (!state.loading && state.allowed && state.mode == LearningMode.ACTIVITY) ActivityAction(state, onEvent)
            },
            contentWindowInsets = WindowInsets(0),
        ) { padding ->
            when {
                state.loading -> LoadingIndicator(Modifier.padding(padding))
                !state.allowed -> Text(
                    stringResource(
                        if (state.module.instrument == Instrument.SINGER) Res.string.learning_not_for_singer else Res.string.learning_not_for_piano,
                    ),
                    Modifier.padding(padding).padding(Spacing.l),
                )
                else -> key(state.mode, state.index, state.module.id) {
                    Column(
                        Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
                        verticalArrangement = Arrangement.spacedBy(Spacing.m),
                    ) {
                        when (state.mode) {
                            LearningMode.HOME -> LearningHome(state, onEvent, navigation)
                            LearningMode.ACTIVITY -> ActivityPage(state, onEvent)
                            LearningMode.SUMMARY -> SessionSummary(state, onEvent)
                        }
                        state.error?.let { Text(it.text(), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        if (state.showSteps) StepsDialog(state, onEvent)
        if (state.showSettings) SettingsDialog(state, onEvent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearningTopBar(state: LearningUiState, onEvent: (LearningEvent) -> Unit, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(state.module.title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = { if (state.mode == LearningMode.HOME) onBack() else onEvent(LearningEvent.Home) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.action_back))
            }
        },
        actions = {
            TextButton(onClick = { onEvent(LearningEvent.Settings) }) { Text(stringResource(Res.string.learning_comfort)) }
        },
        windowInsets = WindowInsets(0),
    )
}

@Composable
private fun ActivityPage(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.learning_step_of, state.index + 1, state.module.activities.size),
            style = MaterialTheme.typography.labelLarge,
        )
        TextButton(onClick = { onEvent(LearningEvent.Steps) }) { Text(stringResource(Res.string.learning_show_steps)) }
    }
    ActivityCard(state, onEvent)
    TextButton(onClick = { onEvent(LearningEvent.Home) }) { Text(stringResource(Res.string.learning_pause)) }
}

private const val LARGE_TEXT_SCALE = 1.2f

@Preview
@Composable
private fun LearningPreview() = AppTheme {
    LearningScreenContent(LearningUiState(loading = false, allowed = true, index = 1, mode = LearningMode.ACTIVITY), {}, LearningNavigation({}))
}
