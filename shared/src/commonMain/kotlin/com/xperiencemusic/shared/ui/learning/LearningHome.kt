package com.xperiencemusic.shared.ui.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.learning.challenge
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.completedIn
import com.xperiencemusic.shared.domain.model.mastery
import com.xperiencemusic.shared.domain.model.returning
import com.xperiencemusic.shared.domain.model.sessionPlan
import com.xperiencemusic.shared.domain.model.weeklyDays
import com.xperiencemusic.shared.presentation.learning.LearningEvent
import com.xperiencemusic.shared.presentation.learning.LearningUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.learning_activity_count
import com.xperiencemusic.shared.resources.learning_challenge_example
import com.xperiencemusic.shared.resources.learning_challenge_example_note
import com.xperiencemusic.shared.resources.learning_challenge_eyebrow
import com.xperiencemusic.shared.resources.learning_challenge_listen
import com.xperiencemusic.shared.resources.learning_challenge_locked
import com.xperiencemusic.shared.resources.learning_challenge_record
import com.xperiencemusic.shared.resources.learning_challenge_step
import com.xperiencemusic.shared.resources.learning_continue_session
import com.xperiencemusic.shared.resources.learning_evaluated_apart_piano
import com.xperiencemusic.shared.resources.learning_evaluated_apart_singer
import com.xperiencemusic.shared.resources.learning_explore_module
import com.xperiencemusic.shared.resources.learning_explore_steps
import com.xperiencemusic.shared.resources.learning_mastery_explain
import com.xperiencemusic.shared.resources.learning_module_count
import com.xperiencemusic.shared.resources.learning_needs_connection
import com.xperiencemusic.shared.resources.learning_next_work
import com.xperiencemusic.shared.resources.learning_path_piano
import com.xperiencemusic.shared.resources.learning_path_singer
import com.xperiencemusic.shared.resources.learning_pending
import com.xperiencemusic.shared.resources.learning_recent_failed
import com.xperiencemusic.shared.resources.learning_recent_passed
import com.xperiencemusic.shared.resources.learning_recent_passed_help
import com.xperiencemusic.shared.resources.learning_recent_row
import com.xperiencemusic.shared.resources.learning_recent_title
import com.xperiencemusic.shared.resources.learning_record_piano
import com.xperiencemusic.shared.resources.learning_record_singer
import com.xperiencemusic.shared.resources.learning_redo_quiz
import com.xperiencemusic.shared.resources.learning_request_review
import com.xperiencemusic.shared.resources.learning_resume
import com.xperiencemusic.shared.resources.learning_rework_hint
import com.xperiencemusic.shared.resources.learning_rhythm_note
import com.xperiencemusic.shared.resources.learning_rhythm_rule
import com.xperiencemusic.shared.resources.learning_rhythm_title
import com.xperiencemusic.shared.resources.learning_scale_eyebrow
import com.xperiencemusic.shared.resources.learning_scale_note
import com.xperiencemusic.shared.resources.learning_scale_text
import com.xperiencemusic.shared.resources.learning_scale_title
import com.xperiencemusic.shared.resources.learning_scale_try
import com.xperiencemusic.shared.resources.learning_start_advice
import com.xperiencemusic.shared.resources.learning_start_session
import com.xperiencemusic.shared.resources.learning_sync
import com.xperiencemusic.shared.resources.learning_today_done
import com.xperiencemusic.shared.resources.learning_today_eyebrow
import com.xperiencemusic.shared.resources.learning_today_first
import com.xperiencemusic.shared.resources.learning_today_plan
import com.xperiencemusic.shared.resources.learning_today_returning
import com.xperiencemusic.shared.resources.learning_today_title
import com.xperiencemusic.shared.resources.learning_weekly_days
import com.xperiencemusic.shared.resources.learning_weekly_target
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.choiceState
import com.xperiencemusic.shared.ui.progress.ExerciseStatusBadge
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** A module's home: today's session, the status of its objective and the rest of the path. */
@Composable
internal fun LearningHome(state: LearningUiState, onEvent: (LearningEvent) -> Unit, navigation: LearningNavigation) {
    TodayCard(state, onEvent)
    if (state.progress.pending > 0) PendingCard(state.progress.pending, onEvent)
    MasteryCard(state, onEvent, navigation.onRecord)
    if (state.module.id == SCALE_MODULE) ScaleCoachCard(navigation.onScaleCoach)
    JourneyGoal(state, onEvent)
    // The starting-point quiz only knows the piano modules.
    if (state.module.instrument == Instrument.PIANO) {
        val startingPoint = state.preferences.startingPoint
        if (startingPoint == null) StartingPoint(state, onEvent) else StartingPointAdvice(state, startingPoint, onEvent, navigation.onModule)
    }
    ChallengeCard(state, onEvent, navigation.onRecord)
    ModuleList(state, navigation.onModule)
    if (state.progress.attempts.isNotEmpty()) RecentAttempts(state)
}

@Composable
private fun TodayCard(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    val activities = state.module.activities
    val plan = state.progress.sessionPlan(state.module, state.today, state.postponed)
    AppCard {
        Text(
            stringResource(Res.string.learning_today_eyebrow),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(Res.string.learning_today_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = Spacing.s),
        )
        val count = pluralStringResource(Res.plurals.learning_activity_count, plan.size, plan.size)
        Text(
            stringResource(Res.string.learning_today_plan, plan.size * MINUTES_PER_ACTIVITY, count, activities.size),
            Modifier.padding(top = Spacing.s),
        )
        Text(stringResource(Res.string.learning_today_first, activities[plan.first()].title), Modifier.padding(top = Spacing.s))
        if (state.progress.returning(state.today)) {
            Text(stringResource(Res.string.learning_today_returning), Modifier.padding(top = Spacing.s))
        }
        Text(
            stringResource(Res.string.learning_today_done, state.completed.size, activities.size),
            Modifier.padding(top = Spacing.m),
        )
        LinearProgressIndicator(
            progress = { state.completed.size.toFloat() / activities.size },
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
        )
        GradientButton(onClick = { onEvent(LearningEvent.Begin) }, modifier = Modifier.padding(top = Spacing.m)) {
            Text(stringResource(if (state.completed.isNotEmpty()) Res.string.learning_continue_session else Res.string.learning_start_session))
        }
        // Only a spot really left behind competes with the main button: never at 0 / 6 on a first visit.
        val resumeIndex = state.resumeIndex
        if (state.canResume && resumeIndex != null) {
            TextButton(onClick = { onEvent(LearningEvent.Resume) }) {
                Text(stringResource(Res.string.learning_resume, activities[resumeIndex.coerceAtMost(state.reached)].title))
            }
        }
        TextButton(onClick = { onEvent(LearningEvent.Steps) }) { Text(stringResource(Res.string.learning_explore_steps)) }
    }
}

@Composable
private fun PendingCard(pending: Int, onEvent: (LearningEvent) -> Unit) {
    AppCard {
        Text(pluralStringResource(Res.plurals.learning_pending, pending, pending))
        TextButton(onClick = { onEvent(LearningEvent.Sync) }) { Text(stringResource(Res.string.learning_sync)) }
    }
}

/** Where the member stands on the module's objective, and how to have it evaluated. */
@Composable
private fun MasteryCard(state: LearningUiState, onEvent: (LearningEvent) -> Unit, onRecord: () -> Unit) {
    val singer = state.module.instrument == Instrument.SINGER
    AppCard {
        val mastery = state.progress.mastery(state.module, state.reviewStatus, state.today)
        var explain by remember { mutableStateOf(false) }
        Text(mastery.label(), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { explain = !explain }) { Text(stringResource(Res.string.learning_mastery_explain)) }
        if (explain) Text(mastery.explanation(), style = MaterialTheme.typography.bodySmall)
        Text(
            stringResource(if (singer) Res.string.learning_evaluated_apart_singer else Res.string.learning_evaluated_apart_piano),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = Spacing.s),
        )
        state.reviewStatus?.let { ExerciseStatusBadge(it, Modifier.padding(top = Spacing.s)) }
        state.reviewFeedback?.let { feedback ->
            Text(
                stringResource(Res.string.learning_next_work),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.s),
            )
            Text(feedback, Modifier.padding(top = Spacing.s))
            if (state.reviewStatus == ExerciseStatus.TO_REWORK) {
                Text(stringResource(Res.string.learning_rework_hint), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (state.complete && state.memberId != null) {
            ObjectiveAudioReview(state.takes) { take, title, subtitle -> onEvent(LearningEvent.PlayTake(take, title, subtitle)) }
        }
        if (state.complete && state.reviewStatus != ExerciseStatus.VALIDATED) {
            OutlinedButton(onClick = onRecord, modifier = Modifier.padding(top = Spacing.m)) {
                Text(stringResource(if (singer) Res.string.learning_record_singer else Res.string.learning_record_piano))
            }
            TextButton(
                onClick = { onEvent(LearningEvent.RequestReview) },
                enabled = state.reviewLoaded && !state.saving && state.reviewStatus != ExerciseStatus.SUBMITTED,
            ) {
                Text(stringResource(Res.string.learning_request_review))
            }
            if (!state.reviewLoaded) {
                Text(stringResource(Res.string.learning_needs_connection), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ScaleCoachCard(onScaleCoach: () -> Unit) {
    AppCard {
        Text(
            stringResource(Res.string.learning_scale_eyebrow),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(Res.string.learning_scale_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(Res.string.learning_scale_text))
        OutlinedButton(onClick = onScaleCoach) { Text(stringResource(Res.string.learning_scale_try)) }
        Text(stringResource(Res.string.learning_scale_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun JourneyGoal(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    val preferences = state.preferences
    val days = state.progress.weeklyDays(state.today)
    AppCard {
        Text(stringResource(Res.string.learning_rhythm_title), style = MaterialTheme.typography.titleMedium)
        Text(pluralStringResource(Res.plurals.learning_weekly_days, days, days, preferences.weeklyTarget))
        Text(stringResource(Res.string.learning_rhythm_rule), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            WEEKLY_TARGETS.forEach { target ->
                FilterChip(
                    selected = preferences.weeklyTarget == target,
                    onClick = { onEvent(LearningEvent.Preferences(preferences.copy(weeklyTarget = target))) },
                    label = { Text(stringResource(Res.string.learning_weekly_target, target)) },
                    modifier = Modifier.choiceState(preferences.weeklyTarget == target),
                )
            }
        }
        Text(stringResource(Res.string.learning_rhythm_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StartingPointAdvice(
    state: LearningUiState,
    startingPoint: String,
    onEvent: (LearningEvent) -> Unit,
    onModule: (String) -> Unit,
) {
    AppCard {
        Text(stringResource(Res.string.learning_start_advice, LearningCatalog.module(startingPoint).title))
        TextButton(onClick = { onModule(startingPoint) }) { Text(stringResource(Res.string.learning_explore_module)) }
        TextButton(onClick = { onEvent(LearningEvent.Preferences(state.preferences.copy(startingPoint = null))) }) {
            Text(stringResource(Res.string.learning_redo_quiz))
        }
    }
}

@Composable
private fun ChallengeCard(state: LearningUiState, onEvent: (LearningEvent) -> Unit, onRecord: () -> Unit) {
    val challenge = state.module.challenge()
    AppCard {
        Text(
            stringResource(Res.string.learning_challenge_eyebrow),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(challenge.title, style = MaterialTheme.typography.titleLarge)
        challenge.steps.forEachIndexed { index, step ->
            Text(stringResource(Res.string.learning_challenge_step, index + 1, step), Modifier.padding(top = Spacing.s))
        }
        Text(stringResource(Res.string.learning_challenge_listen, challenge.criteria), Modifier.padding(top = Spacing.s))
        if (state.module.instrument == Instrument.PIANO) {
            TextButton(onClick = { onEvent(LearningEvent.ChallengeExample) }) { Text(stringResource(Res.string.learning_challenge_example)) }
            Text(stringResource(Res.string.learning_challenge_example_note), style = MaterialTheme.typography.bodySmall)
        }
        if (state.complete && state.reviewStatus != ExerciseStatus.VALIDATED) {
            OutlinedButton(onClick = onRecord) { Text(stringResource(Res.string.learning_challenge_record)) }
        } else if (!state.complete) {
            Text(stringResource(Res.string.learning_challenge_locked), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ModuleList(state: LearningUiState, onModule: (String) -> Unit) {
    Text(
        stringResource(if (state.module.instrument == Instrument.SINGER) Res.string.learning_path_singer else Res.string.learning_path_piano),
        style = MaterialTheme.typography.titleLarge,
    )
    LearningCatalog.modulesFor(state.module.instrument).forEach { module ->
        AppCard(onClick = { onModule(module.id) }) {
            Text(module.title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.learning_module_count, state.progress.completedIn(module).size, module.activities.size),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                state.progress.mastery(module, state.moduleReviews[module.objectiveId], state.today).label(),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RecentAttempts(state: LearningUiState) {
    AppCard {
        Text(stringResource(Res.string.learning_recent_title), style = MaterialTheme.typography.titleMedium)
        state.progress.attempts
            .filter { attempt -> state.module.activities.any { it.id == attempt.activityId } }
            .takeLast(RECENT_ATTEMPTS)
            .reversed()
            .forEach { attempt ->
                val outcome = when {
                    !attempt.passed -> Res.string.learning_recent_failed
                    attempt.hints > 0 -> Res.string.learning_recent_passed_help
                    else -> Res.string.learning_recent_passed
                }
                Text(
                    stringResource(
                        Res.string.learning_recent_row,
                        attempt.day.toString(),
                        LearningCatalog.findActivity(attempt.activityId)?.title.orEmpty(),
                        stringResource(outcome),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.s),
                )
            }
    }
}

private const val SCALE_MODULE = "piano-scales"
private const val MINUTES_PER_ACTIVITY = 2
private const val RECENT_ATTEMPTS = 5
private val WEEKLY_TARGETS = listOf(1, 2, 3)
