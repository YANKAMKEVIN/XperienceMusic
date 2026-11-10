package com.xperiencemusic.shared.ui.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.ServiceKind
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.planning.MonthStatus
import com.xperiencemusic.shared.presentation.planning.MonthValidationUi
import com.xperiencemusic.shared.presentation.planning.MyAnswer
import com.xperiencemusic.shared.presentation.planning.PlanningError
import com.xperiencemusic.shared.presentation.planning.PlanningEvent
import com.xperiencemusic.shared.presentation.planning.PlanningUiState
import com.xperiencemusic.shared.presentation.planning.PlanningViewModel
import com.xperiencemusic.shared.presentation.planning.ServiceCardUi
import com.xperiencemusic.shared.presentation.planning.ServingUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.error_forbidden
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.planning_validate_action
import com.xperiencemusic.shared.resources.planning_validate_all_answered
import com.xperiencemusic.shared.resources.planning_validate_all_covered
import com.xperiencemusic.shared.resources.planning_validate_cancel
import com.xperiencemusic.shared.resources.planning_validate_confirm_title
import com.xperiencemusic.shared.resources.planning_validate_hint
import com.xperiencemusic.shared.resources.planning_validate_incomplete
import com.xperiencemusic.shared.resources.planning_validate_missing_answers
import com.xperiencemusic.shared.resources.planning_validate_missing_members
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.xperiencemusic.shared.ui.components.LoadingIndicator
import androidx.compose.animation.core.tween

@Composable
fun PlanningScreen(
    onMyAnswers: (LocalDate) -> Unit,
    onServiceClick: (LocalDate) -> Unit,
    viewModel: PlanningViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlanningScreenContent(state, viewModel::onEvent, onMyAnswers, onServiceClick)
}

@Composable
fun PlanningScreenContent(
    state: PlanningUiState,
    onEvent: (PlanningEvent) -> Unit,
    onMyAnswers: (LocalDate) -> Unit,
    onServiceClick: (LocalDate) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbar.showSnackbar(getString(if (error == PlanningError.FORBIDDEN) Res.string.error_forbidden else Res.string.error_generic))
            onEvent(PlanningEvent.ErrorShown)
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (state.isLoading) {
            LoadingIndicator()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = Spacing.l, top = Spacing.l, end = Spacing.l, bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                item { PlanningHeader(state, onEvent) }
                // Keyed by month (the cards by date), so changing month fades the old content out, then
                // the new one in, on the same beat as the title: never one frame swap, never both at once.
                item(key = "banner-${state.monthStart}") {
                    Box(Modifier.animateItem(fadeInSpec = monthFadeIn, fadeOutSpec = monthFadeOut)) { MonthBanner(state, onEvent, onMyAnswers) }
                }
                items(state.services, key = { it.date.toString() }) { card ->
                    ServiceCard(
                        card,
                        onClick = { onServiceClick(card.date) },
                        modifier = Modifier.animateItem(fadeInSpec = monthFadeIn, fadeOutSpec = monthFadeOut),
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    state.validation?.takeIf { state.isConfirmingValidation }?.let { validation ->
        ValidateMonthDialog(
            monthLabel = state.monthLabel,
            validation = validation,
            onConfirm = { onEvent(PlanningEvent.ValidateMonthConfirmed) },
            onDismiss = { onEvent(PlanningEvent.ValidateMonthDismissed) },
        )
    }
}

/** Validating locks the members' answers: say what is still missing before doing it. */
@Composable
private fun ValidateMonthDialog(monthLabel: String, validation: MonthValidationUi, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.planning_validate_confirm_title, monthLabel)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(stringResource(Res.string.planning_validate_hint))
                Text(
                    if (validation.missingAnswers > 0) {
                        // Answers (one per member and date), then the people they are missing from: never a bare count.
                        pluralStringResource(Res.plurals.planning_validate_missing_answers, validation.missingAnswers, validation.missingAnswers) + " " +
                            pluralStringResource(Res.plurals.planning_validate_missing_members, validation.missingMembers, validation.missingMembers)
                    } else {
                        stringResource(Res.string.planning_validate_all_answered)
                    },
                    color = if (validation.missingAnswers > 0) AppTheme.colors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (validation.incompleteServices > 0) {
                        pluralStringResource(
                            Res.plurals.planning_validate_incomplete,
                            validation.incompleteServices,
                            validation.incompleteServices,
                            validation.uncoveredParts.map { it.label().lowercase() }.joinToString(", "),
                        )
                    } else {
                        stringResource(Res.string.planning_validate_all_covered)
                    },
                    color = if (validation.incompleteServices > 0) AppTheme.colors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.planning_validate_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.planning_validate_cancel)) }
        },
    )
}

/** The old month leaves quickly, then the new one arrives: title, banner and cards share this beat. */
internal val monthFadeOut = tween<Float>(90)
internal val monthFadeIn = tween<Float>(220, delayMillis = 90)

private val previewCards = listOf(
    ServiceCardUi(
        LocalDate(2026, 11, 1), ServiceKind.SUNDAY, "dim.", 1,
        listOf(
            ServingUi("g", "Grâce", Instrument.SINGER, VoicePart.SOPRANO, false),
            ServingUi("k", "Kevin", Instrument.SINGER, VoicePart.TENOR, true),
            ServingUi("d", "Daniel", Instrument.PIANO, null, false),
        ),
        listOf(Instrument.BASS, Instrument.DRUMS), MyAnswer.SERVES, isPast = false,
    ),
    ServiceCardUi(LocalDate(2026, 11, 6), ServiceKind.FRIDAY, "ven.", 6, emptyList(), listOf(Instrument.SINGER), MyAnswer.UNANSWERED, isPast = false, isToday = true),
)

@Preview
@Composable
private fun PlanningScreenPreview() {
    AppTheme {
        PlanningScreenContent(
            PlanningUiState(
                isLoading = false,
                monthStart = LocalDate(2026, 11, 1),
                monthLabel = "novembre 2026",
                canGoForward = true,
                status = MonthStatus.OPEN,
                answerByLabel = "25 oct.",
                unansweredCount = 3,
                services = previewCards,
                canPlan = true,
                validation = MonthValidationUi(missingAnswers = 22, missingMembers = 9, incompleteServices = 2, uncoveredParts = listOf(Instrument.SINGER, Instrument.BASS, Instrument.DRUMS)),
            ),
            onEvent = {}, onMyAnswers = {}, onServiceClick = {},
        )
    }
}

@Preview
@Composable
private fun ValidateMonthDialogPreview() {
    AppTheme {
        ValidateMonthDialog(
            monthLabel = "novembre 2026",
            validation = MonthValidationUi(missingAnswers = 22, missingMembers = 9, incompleteServices = 2, uncoveredParts = listOf(Instrument.BASS, Instrument.DRUMS)),
            onConfirm = {}, onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun PlanningScreenNotOpenPreview() {
    AppTheme {
        PlanningScreenContent(
            PlanningUiState(isLoading = false, monthLabel = "décembre 2026", status = MonthStatus.NOT_OPEN),
            onEvent = {}, onMyAnswers = {}, onServiceClick = {},
        )
    }
}
