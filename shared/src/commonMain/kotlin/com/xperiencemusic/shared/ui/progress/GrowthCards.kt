package com.xperiencemusic.shared.ui.progress

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
import com.xperiencemusic.shared.presentation.progress.GrowthUnit
import com.xperiencemusic.shared.presentation.progress.LevelCardUi
import com.xperiencemusic.shared.presentation.progress.ObjectiveUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.growth_ask
import com.xperiencemusic.shared.resources.growth_ask_again
import com.xperiencemusic.shared.resources.growth_hint_singer
import com.xperiencemusic.shared.resources.growth_level_to_confirmed
import com.xperiencemusic.shared.resources.growth_level_to_pro
import com.xperiencemusic.shared.resources.growth_objectives
import com.xperiencemusic.shared.resources.growth_objectives_hint
import com.xperiencemusic.shared.resources.growth_objectives_title
import com.xperiencemusic.shared.resources.growth_reached
import com.xperiencemusic.shared.resources.growth_rework
import com.xperiencemusic.shared.resources.growth_tiers
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
                stringResource(
                    if (card.unit == GrowthUnit.TIERS) Res.string.growth_tiers else Res.string.growth_objectives,
                    card.done, card.required, next.target(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
        when {
            card.requestStatus == ExerciseStatus.SUBMITTED -> Note(stringResource(Res.string.growth_waiting), MaterialTheme.colorScheme.primary)
            card.requestStatus == ExerciseStatus.TO_REWORK && card.feedback != null ->
                Note(stringResource(Res.string.growth_rework, card.feedback), MaterialTheme.colorScheme.error)
            !card.canAsk && card.unit == GrowthUnit.TIERS -> Note(stringResource(Res.string.growth_hint_singer), MaterialTheme.colorScheme.onSurfaceVariant)
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

/** A musician's objectives for the next level: tick when reached, the super admin validates. */
@Composable
internal fun ObjectivesCard(next: Level, objectives: List<ObjectiveUi>, isWorking: Boolean, onToggle: (String, Boolean) -> Unit) {
    AppCard {
        Text(stringResource(Res.string.growth_objectives_title, next.target()), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.growth_objectives_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        objectives.forEach { objective ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = Spacing.m)) {
                Column(Modifier.weight(1f)) {
                    Text(objective.title, style = MaterialTheme.typography.titleSmall)
                    Text(objective.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (objective.status == ExerciseStatus.TO_REWORK && objective.feedback != null) {
                        Text("« ${objective.feedback} »", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (objective.status != null) ExerciseStatusBadge(objective.status, Modifier.padding(top = 4.dp))
                }
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
