package com.xperiencemusic.shared.ui.progress

import com.xperiencemusic.shared.resources.growth_learn_title
import com.xperiencemusic.shared.resources.growth_learn_show
import com.xperiencemusic.shared.resources.growth_learn_hide
import com.xperiencemusic.shared.resources.growth_criterion
import com.xperiencemusic.shared.resources.growth_by_microphone
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.presentation.progress.LevelCardUi
import com.xperiencemusic.shared.presentation.progress.ObjectiveUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.growth_ask
import com.xperiencemusic.shared.resources.growth_ask_again
import com.xperiencemusic.shared.resources.growth_level_to_confirmed
import com.xperiencemusic.shared.resources.growth_level_to_pro
import com.xperiencemusic.shared.resources.growth_objectives
import com.xperiencemusic.shared.resources.growth_objectives_hint
import com.xperiencemusic.shared.resources.growth_objectives_title
import com.xperiencemusic.shared.resources.growth_reached
import com.xperiencemusic.shared.resources.growth_rework
import com.xperiencemusic.shared.resources.growth_title
import com.xperiencemusic.shared.resources.growth_top
import com.xperiencemusic.shared.resources.growth_towards
import com.xperiencemusic.shared.resources.growth_waiting
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** "Confirmé", "Pro": the level as it ends "passer …" and "passage …" sentences. */
@Composable
private fun Level.target(): String = stringResource(
    if (this == Level.PRO) Res.string.growth_level_to_pro else Res.string.growth_level_to_confirmed,
)

/** The member's level, how far they are towards the next one, and the request to move up. */
@Composable
internal fun LevelCard(card: LevelCardUi, isWorking: Boolean, onAsk: () -> Unit) {
    AppCard {
        Text(stringResource(Res.string.growth_title), style = MaterialTheme.typography.titleMedium)
        val next = card.next
        if (next == null) {
            Text(
                stringResource(Res.string.growth_top),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            return@AppCard
        }
        Text(
            stringResource(Res.string.growth_towards, card.current.label(), next.label()),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        if (card.required > 0) {
            LinearProgressIndicator(
                progress = { card.done.toFloat() / card.required },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
            )
            Text(
                stringResource(Res.string.growth_objectives, card.done, card.required, next.target()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
        when {
            card.requestStatus == ExerciseStatus.SUBMITTED -> Note(stringResource(Res.string.growth_waiting), MaterialTheme.colorScheme.primary)
            card.requestStatus == ExerciseStatus.TO_REWORK && card.feedback != null ->
                Note(stringResource(Res.string.growth_rework, card.feedback), MaterialTheme.colorScheme.error)
        }
        if (card.canAsk) {
            GradientButton(onClick = onAsk, enabled = !isWorking, modifier = Modifier.fillMaxWidth().padding(top = Spacing.m)) {
                Text(
                    stringResource(
                        if (card.requestStatus == ExerciseStatus.TO_REWORK) Res.string.growth_ask_again else Res.string.growth_ask,
                        next.target(),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun Note(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color, modifier = Modifier.padding(top = Spacing.s))
}

/**
 * The objectives of the next level. Each shows what the super admin checks; a tap opens the help
 * sheet. Members tick what they reached; the microphone validates the singers' tier objectives.
 */
@Composable
internal fun ObjectivesCard(next: Level, objectives: List<ObjectiveUi>, isWorking: Boolean, onToggle: (String, Boolean) -> Unit) {
    var openId by remember { mutableStateOf<String?>(null) }
    AppCard {
        Text(stringResource(Res.string.growth_objectives_title, next.target()), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.growth_objectives_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        objectives.forEachIndexed { index, objective ->
            val isOpen = openId == objective.id
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .padding(top = Spacing.m)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { openId = if (isOpen) null else objective.id },
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${index + 1}. ${objective.title}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(Res.string.growth_criterion, objective.criterion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (objective.status == ExerciseStatus.TO_REWORK && objective.feedback != null) {
                        Text("« ${objective.feedback} »", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        if (objective.status != null) ExerciseStatusBadge(objective.status, Modifier.padding(end = Spacing.s))
                        Text(
                            stringResource(if (isOpen) Res.string.growth_learn_hide else Res.string.growth_learn_show),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    AnimatedVisibility(isOpen) {
                        Column(
                            Modifier
                                .padding(top = Spacing.s)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                                .padding(Spacing.m),
                        ) {
                            Text(stringResource(Res.string.growth_learn_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(objective.learn, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.xs))
                        }
                    }
                }
                if (objective.autoTier != null) {
                    Text(
                        stringResource(Res.string.growth_by_microphone, objective.autoTier),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Spacing.s, top = 4.dp).width(72.dp),
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Checkbox(
                            checked = objective.status == ExerciseStatus.SUBMITTED || objective.status == ExerciseStatus.VALIDATED,
                            onCheckedChange = { onToggle(objective.id, it) },
                            enabled = !isWorking && objective.status != ExerciseStatus.VALIDATED,
                        )
                        Text(stringResource(Res.string.growth_reached), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
