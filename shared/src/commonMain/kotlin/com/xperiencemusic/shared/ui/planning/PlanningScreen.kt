package com.xperiencemusic.shared.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.heading
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
import com.xperiencemusic.shared.resources.planning_unanswered
import com.xperiencemusic.shared.resources.planning_validate_action
import com.xperiencemusic.shared.resources.planning_validate_hint
import com.xperiencemusic.shared.resources.planning_validated
import com.xperiencemusic.shared.resources.planning_validated_text
import com.xperiencemusic.shared.resources.planning_you_serve
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = Spacing.l, top = Spacing.l, end = Spacing.l, bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                item { Header(state, onEvent) }
                item { MonthBanner(state, onEvent, onMyAnswers) }
                items(state.services, key = { it.date.toString() }) { card ->
                    ServiceCard(card, onClick = { onServiceClick(card.date) })
                }
                if (state.canPlan && state.status == MonthStatus.OPEN) {
                    item {
                        Column(Modifier.padding(top = Spacing.s)) {
                            GradientButton(
                                onClick = { onEvent(PlanningEvent.ValidateMonthClicked) },
                                enabled = !state.isWorking,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(Res.string.planning_validate_action), style = MaterialTheme.typography.titleMedium) }
                            Text(
                                stringResource(Res.string.planning_validate_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.xs).align(Alignment.CenterHorizontally),
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Header(state: PlanningUiState, onEvent: (PlanningEvent) -> Unit) {
    Column {
        Text(
            stringResource(Res.string.planning_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.xs)) {
            IconButton(onClick = { onEvent(PlanningEvent.PreviousMonth) }, enabled = state.canGoBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.planning_previous_month))
            }
            Text(
                state.monthLabel.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
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
                    Text(stringResource(Res.string.planning_open_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.answerByLabel?.let { stringResource(Res.string.planning_open_text_by, it) }
                            ?: stringResource(Res.string.planning_open_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.unansweredCount > 0) {
                        Text(
                            stringResource(Res.string.planning_unanswered, state.unansweredCount),
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
        }
        MonthStatus.VALIDATED -> AppCard {
            Text(stringResource(Res.string.planning_validated), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
            Text(
                stringResource(Res.string.planning_validated_text),
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
private fun ServiceCard(card: ServiceCardUi, onClick: () -> Unit) {
    AppCard(onClick = onClick, modifier = Modifier.alpha(if (card.isPast) 0.55f else 1f)) {
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
                        modifier = Modifier.weight(1f),
                    )
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
                MissingLine(card.missing, Modifier.padding(top = Spacing.s))
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
    ServiceCardUi(LocalDate(2026, 11, 6), ServiceKind.FRIDAY, "ven.", 6, emptyList(), listOf(Instrument.SINGER), MyAnswer.UNANSWERED, isPast = false),
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
            ),
            onEvent = {}, onMyAnswers = {}, onServiceClick = {},
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
