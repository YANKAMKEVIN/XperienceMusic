package com.xperiencemusic.shared.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.xperiencemusic.shared.resources.planning_complete
import com.xperiencemusic.shared.resources.planning_friday
import com.xperiencemusic.shared.resources.planning_late_text
import com.xperiencemusic.shared.resources.planning_late_title
import com.xperiencemusic.shared.resources.planning_missing
import com.xperiencemusic.shared.resources.planning_my_answers
import com.xperiencemusic.shared.resources.planning_next_month
import com.xperiencemusic.shared.resources.planning_nobody
import com.xperiencemusic.shared.resources.planning_not_available
import com.xperiencemusic.shared.resources.planning_not_open_admin
import com.xperiencemusic.shared.resources.planning_not_open_member
import com.xperiencemusic.shared.resources.planning_not_open_title
import com.xperiencemusic.shared.resources.planning_open_action
import com.xperiencemusic.shared.resources.planning_open_text
import com.xperiencemusic.shared.resources.planning_open_text_by
import com.xperiencemusic.shared.resources.planning_open_title
import com.xperiencemusic.shared.resources.planning_previous_month
import com.xperiencemusic.shared.resources.planning_sunday
import com.xperiencemusic.shared.resources.planning_title
import com.xperiencemusic.shared.resources.planning_to_answer
import com.xperiencemusic.shared.resources.planning_today
import com.xperiencemusic.shared.resources.planning_unanswered
import com.xperiencemusic.shared.resources.planning_validate_action
import com.xperiencemusic.shared.resources.planning_validate_all_answered
import com.xperiencemusic.shared.resources.planning_validate_all_covered
import com.xperiencemusic.shared.resources.planning_validate_cancel
import com.xperiencemusic.shared.resources.planning_validate_confirm_title
import com.xperiencemusic.shared.resources.planning_validate_hint
import com.xperiencemusic.shared.resources.planning_validate_incomplete
import com.xperiencemusic.shared.resources.planning_validate_missing_answers
import com.xperiencemusic.shared.resources.planning_validate_missing_members
import com.xperiencemusic.shared.resources.planning_validated
import com.xperiencemusic.shared.resources.planning_validated_incomplete
import com.xperiencemusic.shared.resources.planning_validated_text
import com.xperiencemusic.shared.resources.planning_you_serve
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.brand.TabHeader
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.xperiencemusic.shared.ui.components.LoadingIndicator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextOverflow

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
                item { Header(state, onEvent) }
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
                    color = if (validation.missingAnswers > 0) BrandColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = if (validation.incompleteServices > 0) BrandColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant,
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
private val monthFadeOut = tween<Float>(90)
private val monthFadeIn = tween<Float>(220, delayMillis = 90)

@Composable
private fun Header(state: PlanningUiState, onEvent: (PlanningEvent) -> Unit) {
    Column {
        TabHeader(stringResource(Res.string.planning_title))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.xs)) {
            IconButton(onClick = { onEvent(PlanningEvent.PreviousMonth) }, enabled = state.canGoBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.planning_previous_month))
            }
            // The month slides the way the arrows point.
            AnimatedContent(
                targetState = state.monthStart to state.monthLabel,
                transitionSpec = {
                    val from = initialState.first
                    val to = targetState.first
                    val direction = if (from != null && to != null && to < from) -1 else 1
                    // The old title is gone before the new one shows, so the two never overlap.
                    (slideInHorizontally(tween(220, delayMillis = 90)) { it / 3 * direction } + fadeIn(monthFadeIn))
                        .togetherWith(slideOutHorizontally(tween(90)) { -it / 3 * direction } + fadeOut(monthFadeOut))
                },
                contentAlignment = Alignment.Center,
                label = "month",
                modifier = Modifier.weight(1f),
            ) { (_, label) ->
                Text(
                    label.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            IconButton(onClick = { onEvent(PlanningEvent.NextMonth) }, enabled = state.canGoForward) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(Res.string.planning_next_month))
            }
        }
    }
}

@Composable
private fun MonthBanner(state: PlanningUiState, onEvent: (PlanningEvent) -> Unit, onMyAnswers: (LocalDate) -> Unit) {
    when (state.status) {
        MonthStatus.NOT_OPEN -> EmptyState(
            icon = XmIcons.Calendar,
            title = stringResource(Res.string.planning_not_open_title),
            text = stringResource(if (state.canPlan) Res.string.planning_not_open_admin else Res.string.planning_not_open_member),
            actionLabel = if (state.canPlan) stringResource(Res.string.planning_open_action) else null,
            onAction = { onEvent(PlanningEvent.OpenMonthClicked) },
        )
        MonthStatus.OPEN -> AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(XmIcons.Calendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
                    // A deadline already behind us is said as such, not as a date still to come.
                    val late = state.isPastDeadline && state.answerByLabel != null
                    Text(
                        stringResource(if (late) Res.string.planning_late_title else Res.string.planning_open_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (late) BrandColors.Amber else Color.Unspecified,
                    )
                    Text(
                        when {
                            late -> stringResource(Res.string.planning_late_text, state.answerByLabel.orEmpty())
                            state.answerByLabel != null -> stringResource(Res.string.planning_open_text_by, state.answerByLabel)
                            else -> stringResource(Res.string.planning_open_text)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.unansweredCount > 0) {
                        Text(
                            pluralStringResource(Res.plurals.planning_unanswered, state.unansweredCount, state.unansweredCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandColors.Amber,
                            modifier = Modifier.padding(top = Spacing.xs),
                        )
                    }
                }
            }
            GradientButton(
                onClick = { state.monthStart?.let(onMyAnswers) },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.m),
            ) { Text(stringResource(Res.string.planning_my_answers), style = MaterialTheme.typography.titleMedium) }
            // At the top for the admin, rather than after every service of the month; it asks first.
            if (state.validation != null) {
                OutlinedButton(
                    onClick = { onEvent(PlanningEvent.ValidateMonthClicked) },
                    enabled = !state.isWorking,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
                ) { Text(stringResource(Res.string.planning_validate_action)) }
            }
        }
        MonthStatus.VALIDATED -> AppCard {
            Text(stringResource(Res.string.planning_validated), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
            // "Planning validé" next to "Manque : …" on the dates needs a word: fixed, yet not complete.
            Text(
                if (state.incompleteAfterValidation > 0) {
                    pluralStringResource(Res.plurals.planning_validated_incomplete, state.incompleteAfterValidation, state.incompleteAfterValidation)
                } else {
                    stringResource(Res.string.planning_validated_text)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            if (state.canPlan) {
                OutlinedButton(
                    onClick = { state.monthStart?.let(onMyAnswers) },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
                ) { Text(stringResource(Res.string.planning_my_answers)) }
            }
        }
    }
}

/** A Friday or Sunday: big date on the left, the team, what is missing, and the member's answer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceCard(card: ServiceCardUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(onClick = onClick, modifier = modifier.alpha(if (card.isPast) 0.55f else 1f)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
                Text(card.weekday, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(card.day.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f).padding(start = Spacing.s)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (card.kind == ServiceKind.FRIDAY) Res.string.planning_friday else Res.string.planning_sunday),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (card.isToday) TodayPill(Modifier.padding(start = Spacing.s))
                    Spacer(Modifier.weight(1f))
                    if (card.showsAnswer) AnswerBadge(card.myAnswer)
                }
                if (card.serving.isEmpty()) {
                    Text(
                        stringResource(Res.string.planning_nobody),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.padding(top = Spacing.s),
                    ) { card.serving.forEach { PersonPill(it) } }
                }
                if (!card.isPast) MissingLine(card.missing, Modifier.padding(top = Spacing.s))
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
internal fun MissingLine(missing: List<Instrument>, modifier: Modifier = Modifier) {
    if (missing.isEmpty()) {
        Text(stringResource(Res.string.planning_complete), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary, modifier = modifier)
    } else {
        Text(
            stringResource(Res.string.planning_missing, missing.map { it.label().lowercase() }.joinToString(", ")),
            style = MaterialTheme.typography.labelMedium,
            color = BrandColors.Amber,
            modifier = modifier,
        )
    }
}

@Composable
private fun TodayPill(modifier: Modifier = Modifier) {
    Text(
        stringResource(Res.string.planning_today),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.background(BrandColors.horizontalGradient, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
private fun AnswerBadge(answer: MyAnswer) {
    val (label, color) = when (answer) {
        MyAnswer.SERVES -> stringResource(Res.string.planning_you_serve) to MaterialTheme.colorScheme.tertiary
        MyAnswer.NOT_AVAILABLE -> stringResource(Res.string.planning_not_available) to MaterialTheme.colorScheme.onSurfaceVariant
        MyAnswer.UNANSWERED -> stringResource(Res.string.planning_to_answer) to MaterialTheme.colorScheme.primary
    }
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** "Kevin · ténor", the signed-in member highlighted. */
@Composable
internal fun PersonPill(person: ServingUi) {
    val part = person.voicePart?.label()?.lowercase() ?: person.instrument?.label()?.lowercase()
    val color = if (person.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Text(
        listOfNotNull(person.name, part).joinToString(" · "),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(
                if (person.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                CircleShape,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

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
