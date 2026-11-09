package com.xperiencemusic.shared.ui.planning

import com.xperiencemusic.shared.resources.availability_past_unanswered
import com.xperiencemusic.shared.resources.availability_past_unavailable
import com.xperiencemusic.shared.resources.availability_past_served
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.ServiceKind
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.planning.AvailabilityEvent
import com.xperiencemusic.shared.presentation.planning.AvailabilityRowUi
import com.xperiencemusic.shared.presentation.planning.AvailabilityUiState
import com.xperiencemusic.shared.presentation.planning.AvailabilityViewModel
import com.xperiencemusic.shared.presentation.planning.MyAnswer
import com.xperiencemusic.shared.presentation.planning.PlanningError
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.availability_answer_by
import com.xperiencemusic.shared.resources.availability_late
import com.xperiencemusic.shared.resources.availability_locked
import com.xperiencemusic.shared.resources.availability_role
import com.xperiencemusic.shared.resources.availability_saved_hint
import com.xperiencemusic.shared.resources.availability_serve
import com.xperiencemusic.shared.resources.availability_title
import com.xperiencemusic.shared.resources.availability_unavailable
import com.xperiencemusic.shared.resources.error_forbidden
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.planning_friday_hint
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.choiceState
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Palette
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.xperiencemusic.shared.ui.components.LoadingIndicator

@Composable
fun AvailabilityScreen(
    monthStart: LocalDate,
    onBack: () -> Unit,
    viewModel: AvailabilityViewModel = koinViewModel(key = "availability-$monthStart") { parametersOf(monthStart) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AvailabilityScreenContent(state, viewModel::onEvent, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityScreenContent(state: AvailabilityUiState, onEvent: (AvailabilityEvent) -> Unit, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbar.showSnackbar(getString(if (error == PlanningError.FORBIDDEN) Res.string.error_forbidden else Res.string.error_generic))
            onEvent(AvailabilityEvent.ErrorShown)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.availability_title, state.monthLabel)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(0),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (state.isLoading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            item { Intro(state) }
            items(state.rows, key = { it.date.toString() }) { row ->
                AnswerRow(row, enabled = state.canAnswer && !row.isPast && state.savingDate == null, onEvent)
            }
        }
    }
}

@Composable
private fun Intro(state: AvailabilityUiState) {
    AppCard {
        val part = state.voicePart?.label()?.lowercase()?.let { voice -> "${state.instrument?.label()?.lowercase().orEmpty()} $voice".trim() }
            ?: state.instrument?.label()?.lowercase()
        if (part != null) Text(stringResource(Res.string.availability_role, part), style = MaterialTheme.typography.bodyMedium)
        val note = when {
            !state.canAnswer -> stringResource(Res.string.availability_locked)
            state.isPastDeadline && state.answerByLabel != null -> stringResource(Res.string.availability_late, state.answerByLabel) + "\n" +
                stringResource(Res.string.availability_saved_hint)
            state.answerByLabel != null -> stringResource(Res.string.availability_answer_by, state.answerByLabel) + "\n" +
                stringResource(Res.string.availability_saved_hint)
            else -> stringResource(Res.string.availability_saved_hint)
        }
        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.xs))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnswerRow(row: AvailabilityRowUi, enabled: Boolean, onEvent: (AvailabilityEvent) -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.label.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                color = if (row.isPast) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                modifier = Modifier.weight(1f),
            )
            if (row.kind == ServiceKind.FRIDAY) {
                Text(
                    stringResource(Res.string.planning_friday_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // A date gone by can no longer be answered: say so and what was answered, instead of dead buttons.
        if (row.isPast) {
            Text(
                stringResource(
                    when (row.answer) {
                        MyAnswer.SERVES -> Res.string.availability_past_served
                        MyAnswer.NOT_AVAILABLE -> Res.string.availability_past_unavailable
                        MyAnswer.UNANSWERED -> Res.string.availability_past_unanswered
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.s),
            )
            return@AppCard
        }
        val options = listOf(true to Res.string.availability_serve, false to Res.string.availability_unavailable)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = Spacing.s)) {
            options.forEachIndexed { index, (serves, label) ->
                val selected = (row.answer == MyAnswer.SERVES) == serves && row.answer != MyAnswer.UNANSWERED
                // "Je sers" turns green and "Pas dispo" grey: the two answers no longer share one blue.
                val tint = if (serves) Palette.Success else Palette.Mist
                SegmentedButton(
                    selected = selected,
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = tint.copy(alpha = 0.22f),
                        activeContentColor = if (serves) Palette.Success else Palette.Snow,
                        activeBorderColor = tint,
                    ),
                    onClick = { onEvent(AvailabilityEvent.Answered(row.date, serves)) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    icon = { if (selected) Icon(Icons.Filled.Check, contentDescription = null) },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.choiceState(selected),
                )
            }
        }
    }
}

@Preview
@Composable
private fun AvailabilityScreenPreview() {
    AppTheme {
        AvailabilityScreenContent(
            AvailabilityUiState(
                isLoading = false,
                monthLabel = "novembre 2026",
                answerByLabel = "25 oct.",
                instrument = Instrument.SINGER,
                voicePart = VoicePart.TENOR,
                canAnswer = true,
                rows = listOf(
                    AvailabilityRowUi(LocalDate(2026, 11, 1), ServiceKind.SUNDAY, "dimanche 1 nov.", MyAnswer.SERVES, false),
                    AvailabilityRowUi(LocalDate(2026, 11, 6), ServiceKind.FRIDAY, "vendredi 6 nov.", MyAnswer.UNANSWERED, false),
                    AvailabilityRowUi(LocalDate(2026, 11, 8), ServiceKind.SUNDAY, "dimanche 8 nov.", MyAnswer.NOT_AVAILABLE, false),
                ),
            ),
            onEvent = {}, onBack = {},
        )
    }
}
