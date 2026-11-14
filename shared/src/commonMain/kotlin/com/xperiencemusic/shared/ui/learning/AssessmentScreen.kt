package com.xperiencemusic.shared.ui.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.SubmissionOutcome
import com.xperiencemusic.shared.presentation.learning.AssessmentError
import com.xperiencemusic.shared.presentation.learning.AssessmentUiState
import com.xperiencemusic.shared.presentation.learning.AssessmentViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.assessment_denied_piano
import com.xperiencemusic.shared.resources.assessment_denied_singer
import com.xperiencemusic.shared.resources.assessment_error_mic
import com.xperiencemusic.shared.resources.assessment_error_send
import com.xperiencemusic.shared.resources.assessment_instructions_piano
import com.xperiencemusic.shared.resources.assessment_instructions_singer
import com.xperiencemusic.shared.resources.assessment_kept
import com.xperiencemusic.shared.resources.assessment_listen
import com.xperiencemusic.shared.resources.assessment_record
import com.xperiencemusic.shared.resources.assessment_recording
import com.xperiencemusic.shared.resources.assessment_redo
import com.xperiencemusic.shared.resources.assessment_rehearsal
import com.xperiencemusic.shared.resources.assessment_send
import com.xperiencemusic.shared.resources.assessment_sending
import com.xperiencemusic.shared.resources.assessment_sent
import com.xperiencemusic.shared.resources.assessment_stop
import com.xperiencemusic.shared.resources.assessment_take_title
import com.xperiencemusic.shared.resources.assessment_title_piano
import com.xperiencemusic.shared.resources.assessment_title_singer
import com.xperiencemusic.shared.resources.assessment_too_short
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.MicrophoneDeniedNotice
import com.xperiencemusic.shared.ui.platform.MicrophoneUse
import com.xperiencemusic.shared.ui.platform.rememberMicrophonePermission
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** What the take screen can do; the defaults do nothing, for previews. */
class AssessmentActions(
    val start: () -> Unit = {},
    val stop: () -> Unit = {},
    val listen: (title: String) -> Unit = {},
    val send: () -> Unit = {},
    val openSettings: () -> Unit = {},
)

@Composable
fun AssessmentScreen(
    moduleId: String,
    onBack: () -> Unit,
    viewModel: AssessmentViewModel = koinViewModel(key = "assessment-$moduleId") { parametersOf(moduleId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberMicrophonePermission(MicrophoneUse.RECORD)
    AssessmentScreenContent(
        state,
        onBack,
        AssessmentActions(
            start = { permission.request { if (it) viewModel.start() else viewModel.denied() } },
            stop = viewModel::stop,
            listen = viewModel::listen,
            send = viewModel::send,
            openSettings = permission.openSettings,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentScreenContent(state: AssessmentUiState, onBack: () -> Unit, actions: AssessmentActions = AssessmentActions()) {
    val singer = state.module.instrument == Instrument.SINGER
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (singer) Res.string.assessment_title_singer else Res.string.assessment_title_piano)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.action_back)) }
                },
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            AppCard {
                Text(state.module.title, style = MaterialTheme.typography.headlineSmall)
                Text(state.module.activities.last().explanation, Modifier.padding(top = Spacing.m))
                Text(
                    stringResource(if (singer) Res.string.assessment_instructions_singer else Res.string.assessment_instructions_piano),
                    Modifier.padding(top = Spacing.m),
                )
            }
            if (state.level.isRecording) Recording(state, actions) else TakeControls(state, actions)
            AssessmentMessage(state, singer, actions.openSettings)
            Text(stringResource(Res.string.assessment_rehearsal), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Recording(state: AssessmentUiState, actions: AssessmentActions) {
    Text(stringResource(Res.string.assessment_recording, state.level.seconds.toInt()), style = MaterialTheme.typography.headlineSmall)
    LinearProgressIndicator(progress = { state.level.level }, modifier = Modifier.fillMaxWidth())
    GradientButton(onClick = actions.stop) { Text(stringResource(Res.string.assessment_stop)) }
}

@Composable
private fun TakeControls(state: AssessmentUiState, actions: AssessmentActions) {
    val seconds = state.recordedSeconds
    GradientButton(onClick = actions.start, enabled = !state.sending) {
        Text(stringResource(if (seconds > 0) Res.string.assessment_redo else Res.string.assessment_record))
    }
    if (seconds >= MIN_SECONDS) {
        val title = stringResource(Res.string.assessment_take_title)
        OutlinedButton(onClick = { actions.listen(title) }, enabled = !state.sending) {
            Text(stringResource(Res.string.assessment_listen, seconds))
        }
        OutlinedButton(onClick = actions.send, enabled = !state.sending) {
            Text(stringResource(if (state.sending) Res.string.assessment_sending else Res.string.assessment_send))
        }
    } else if (seconds > 0) {
        Text(stringResource(Res.string.assessment_too_short))
    }
}

@Composable
private fun AssessmentMessage(state: AssessmentUiState, singer: Boolean, openSettings: () -> Unit) {
    val error = state.error
    if (error == AssessmentError.MICROPHONE_DENIED) {
        MicrophoneDeniedNotice(
            stringResource(if (singer) Res.string.assessment_denied_singer else Res.string.assessment_denied_piano),
            openSettings,
        )
        return
    }
    val message = when {
        error == AssessmentError.SEND_FAILED -> Res.string.assessment_error_send
        error == AssessmentError.MICROPHONE_UNAVAILABLE -> Res.string.assessment_error_mic
        state.outcome == SubmissionOutcome.REVIEW_REQUESTED -> Res.string.assessment_sent
        state.outcome == SubmissionOutcome.TAKE_KEPT -> Res.string.assessment_kept
        else -> null
    }
    message?.let { Text(stringResource(it)) }
}

private const val MIN_SECONDS = 3

@Preview
@Composable
private fun AssessmentPreview() = AppTheme {
    AssessmentScreenContent(AssessmentUiState(LearningCatalog.module(LearningCatalog.DEFAULT), recordedSeconds = 12), onBack = {})
}
