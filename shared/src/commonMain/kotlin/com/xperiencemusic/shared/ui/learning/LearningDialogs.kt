package com.xperiencemusic.shared.ui.learning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.model.LearningActivity
import com.xperiencemusic.shared.domain.model.LearningPreferences
import com.xperiencemusic.shared.presentation.learning.LearningEvent
import com.xperiencemusic.shared.presentation.learning.LearningUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.learning_activities_worked
import com.xperiencemusic.shared.resources.learning_close
import com.xperiencemusic.shared.resources.learning_setting_haptics
import com.xperiencemusic.shared.resources.learning_setting_large_text
import com.xperiencemusic.shared.resources.learning_setting_reduce_motion
import com.xperiencemusic.shared.resources.learning_setting_reward_sound
import com.xperiencemusic.shared.resources.learning_setting_touch_sound
import com.xperiencemusic.shared.resources.learning_settings_done
import com.xperiencemusic.shared.resources.learning_settings_note
import com.xperiencemusic.shared.resources.learning_settings_title
import com.xperiencemusic.shared.resources.learning_step_done
import com.xperiencemusic.shared.resources.learning_step_later
import com.xperiencemusic.shared.resources.learning_step_locked
import com.xperiencemusic.shared.resources.learning_step_locked_state
import com.xperiencemusic.shared.resources.learning_step_number
import com.xperiencemusic.shared.resources.learning_steps_rule
import com.xperiencemusic.shared.resources.learning_steps_title
import com.xperiencemusic.shared.resources.learning_summary_continue
import com.xperiencemusic.shared.resources.learning_summary_difficult
import com.xperiencemusic.shared.resources.learning_summary_done
import com.xperiencemusic.shared.resources.learning_summary_easy
import com.xperiencemusic.shared.resources.learning_summary_finish
import com.xperiencemusic.shared.resources.learning_summary_next
import com.xperiencemusic.shared.resources.learning_summary_title
import com.xperiencemusic.shared.resources.learning_summary_to_rework
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import com.xperiencemusic.shared.ui.theme.appSwitchColors
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The end of a session: what was worked, what to take up again, and what comes next. */
@Composable
internal fun SessionSummary(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    fun title(id: String) = state.sessionTitles[id] ?: LearningCatalog.findActivity(id)?.title.orEmpty()
    AppCard {
        Text(stringResource(Res.string.learning_summary_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            pluralStringResource(Res.plurals.learning_activities_worked, state.sessionDone.size, state.sessionDone.size),
            Modifier.padding(top = Spacing.m),
        )
        // An activity to take up again is listed once, below, not also as a success.
        state.sessionDone.filterNot { it in state.sessionDifficult }.forEach { id ->
            Text(stringResource(Res.string.learning_summary_done, title(id)), Modifier.padding(top = Spacing.s))
        }
        if (state.sessionDifficult.isNotEmpty()) {
            Text(
                stringResource(Res.string.learning_summary_to_rework),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.m),
            )
            state.sessionDifficult.forEach { id -> Text(title(id)) }
        }
        Text(
            stringResource(if (state.sessionDifficult.isEmpty()) Res.string.learning_summary_easy else Res.string.learning_summary_difficult),
            Modifier.padding(top = Spacing.m),
        )
        Text(
            stringResource(Res.string.learning_summary_next, state.module.activities[state.reached].title),
            Modifier.padding(top = Spacing.s),
        )
    }
    GradientButton(onClick = { onEvent(LearningEvent.Home) }) { Text(stringResource(Res.string.learning_summary_finish)) }
    OutlinedButton(onClick = { onEvent(LearningEvent.Open(state.reached)) }) { Text(stringResource(Res.string.learning_summary_continue)) }
}

@Composable
internal fun StepsDialog(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(LearningEvent.Steps) },
        title = { Text(stringResource(Res.string.learning_steps_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                state.module.activities.forEachIndexed { index, activity -> StepRow(state, index, activity, onEvent) }
                if (!state.complete && state.reached < state.module.activities.lastIndex) {
                    Text(
                        stringResource(Res.string.learning_steps_rule),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = Spacing.s),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(LearningEvent.Steps) }) { Text(stringResource(Res.string.learning_close)) }
        },
    )
}

@Composable
private fun StepRow(state: LearningUiState, index: Int, activity: LearningActivity, onEvent: (LearningEvent) -> Unit) {
    val unlocked = state.complete || index <= state.reached
    val later = activity.id in state.postponed && activity.id !in state.completed
    val name = if (activity.id in state.completed) {
        stringResource(Res.string.learning_step_done, activity.title)
    } else {
        stringResource(Res.string.learning_step_number, index + 1, activity.title)
    }
    val label = if (later) stringResource(Res.string.learning_step_later, name) else name
    // A step still closed is plain text, read as such: no button that does nothing.
    if (unlocked) {
        TextButton(onClick = { onEvent(LearningEvent.Open(index)) }) { Text(label) }
    } else {
        val closed = stringResource(Res.string.learning_step_locked_state)
        Text(
            stringResource(Res.string.learning_step_locked, label),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CHOICE_MIN_HEIGHT)
                .padding(horizontal = Spacing.m, vertical = Spacing.sm)
                .semantics { stateDescription = closed },
        )
    }
}

@Composable
internal fun SettingsDialog(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    val preferences = state.preferences
    fun change(value: LearningPreferences) = onEvent(LearningEvent.Preferences(value))
    AlertDialog(
        onDismissRequest = { onEvent(LearningEvent.Settings) },
        title = { Text(stringResource(Res.string.learning_settings_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Setting(stringResource(Res.string.learning_setting_touch_sound), preferences.touchSound) {
                    change(preferences.copy(touchSound = it))
                }
                Setting(stringResource(Res.string.learning_setting_reward_sound), preferences.rewardSound) {
                    change(preferences.copy(rewardSound = it))
                }
                Setting(stringResource(Res.string.learning_setting_haptics), preferences.haptics) {
                    change(preferences.copy(haptics = it))
                }
                Setting(stringResource(Res.string.learning_setting_reduce_motion), preferences.reduceMotion) {
                    change(preferences.copy(reduceMotion = it))
                }
                Setting(stringResource(Res.string.learning_setting_large_text), preferences.largeText) {
                    change(preferences.copy(largeText = it))
                }
                Text(stringResource(Res.string.learning_settings_note), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(LearningEvent.Settings) }) { Text(stringResource(Res.string.learning_settings_done)) }
        },
    )
}

/** The whole row is the switch: TalkBack reads its label and state, and a tap on the label toggles it. */
@Composable
private fun Setting(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Sizes.touchTarget).toggleable(value, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(value, onCheckedChange = null, colors = appSwitchColors())
    }
}

private val CHOICE_MIN_HEIGHT = 40.dp
