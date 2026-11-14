package com.xperiencemusic.shared.ui.planning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.xperiencemusic.shared.presentation.planning.MonthStatus
import com.xperiencemusic.shared.presentation.planning.PlanningEvent
import com.xperiencemusic.shared.presentation.planning.PlanningUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.planning_late_text
import com.xperiencemusic.shared.resources.planning_late_title
import com.xperiencemusic.shared.resources.planning_my_answers
import com.xperiencemusic.shared.resources.planning_next_month
import com.xperiencemusic.shared.resources.planning_not_open_admin
import com.xperiencemusic.shared.resources.planning_not_open_member
import com.xperiencemusic.shared.resources.planning_not_open_title
import com.xperiencemusic.shared.resources.planning_open_action
import com.xperiencemusic.shared.resources.planning_open_text
import com.xperiencemusic.shared.resources.planning_open_text_by
import com.xperiencemusic.shared.resources.planning_open_title
import com.xperiencemusic.shared.resources.planning_previous_month
import com.xperiencemusic.shared.resources.planning_title
import com.xperiencemusic.shared.resources.planning_unanswered
import com.xperiencemusic.shared.resources.planning_validate_action
import com.xperiencemusic.shared.resources.planning_validated
import com.xperiencemusic.shared.resources.planning_validated_incomplete
import com.xperiencemusic.shared.resources.planning_validated_text
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.brand.TabHeader
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.format.rememberFormattingWords
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween

@Composable
internal fun PlanningHeader(state: PlanningUiState, onEvent: (PlanningEvent) -> Unit) {
    val words = rememberFormattingWords()
    Column {
        TabHeader(stringResource(Res.string.planning_title))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.xs)) {
            IconButton(onClick = { onEvent(PlanningEvent.PreviousMonth) }, enabled = state.canGoBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.planning_previous_month))
            }
            // The month slides the way the arrows point.
            AnimatedContent(
                targetState = state.monthStart,
                transitionSpec = {
                    val from = initialState
                    val to = targetState
                    val direction = if (from != null && to != null && to < from) -1 else 1
                    // The old title is gone before the new one shows, so the two never overlap.
                    (slideInHorizontally(tween(220, delayMillis = 90)) { it / 3 * direction } + fadeIn(monthFadeIn))
                        .togetherWith(slideOutHorizontally(tween(90)) { -it / 3 * direction } + fadeOut(monthFadeOut))
                },
                contentAlignment = Alignment.Center,
                label = "month",
                modifier = Modifier.weight(1f),
            ) { monthStart ->
                Text(
                    monthStart?.let(words::monthAndYear).orEmpty().replaceFirstChar { it.uppercase() },
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
internal fun MonthBanner(state: PlanningUiState, onEvent: (PlanningEvent) -> Unit, onMyAnswers: (LocalDate) -> Unit) {
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
                    val answerBy = state.answerBy?.let { rememberFormattingWords().dayAndMonth(it) }
                    val late = state.isPastDeadline && answerBy != null
                    Text(
                        stringResource(if (late) Res.string.planning_late_title else Res.string.planning_open_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (late) AppTheme.colors.warning else Color.Unspecified,
                    )
                    Text(
                        when {
                            late -> stringResource(Res.string.planning_late_text, answerBy.orEmpty())
                            answerBy != null -> stringResource(Res.string.planning_open_text_by, answerBy)
                            else -> stringResource(Res.string.planning_open_text)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.unansweredCount > 0) {
                        Text(
                            pluralStringResource(Res.plurals.planning_unanswered, state.unansweredCount, state.unansweredCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = AppTheme.colors.warning,
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
