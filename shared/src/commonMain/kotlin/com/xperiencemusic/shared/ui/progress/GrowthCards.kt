package com.xperiencemusic.shared.ui.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.presentation.progress.ModuleProgressUi
import com.xperiencemusic.shared.presentation.progress.ObjectiveUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.growth_by_microphone
import com.xperiencemusic.shared.resources.growth_level_to_confirmed
import com.xperiencemusic.shared.resources.growth_level_to_pro
import com.xperiencemusic.shared.resources.growth_learn_title
import com.xperiencemusic.shared.resources.progress_module_count
import com.xperiencemusic.shared.resources.progress_objective_module
import com.xperiencemusic.shared.resources.progress_objective_number
import com.xperiencemusic.shared.resources.progress_objective_todo
import com.xperiencemusic.shared.resources.progress_sheet_criterion
import com.xperiencemusic.shared.resources.progress_sheet_learn
import com.xperiencemusic.shared.resources.progress_sheet_reached
import com.xperiencemusic.shared.resources.progress_sheet_reached_hint
import com.xperiencemusic.shared.resources.status_submitted
import com.xperiencemusic.shared.resources.status_to_rework
import com.xperiencemusic.shared.resources.status_validated
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Palette
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** "Confirmé", "Pro": the level as it ends "passer …" and "vers …" sentences. */
@Composable
internal fun Level.target(): String = stringResource(
    if (this == Level.PRO) Res.string.growth_level_to_pro else Res.string.growth_level_to_confirmed,
)

internal fun ObjectiveUi.segment(): RingSegment = when (status) {
    ExerciseStatus.VALIDATED -> RingSegment.VALIDATED
    ExerciseStatus.SUBMITTED -> RingSegment.WAITING
    ExerciseStatus.TO_REWORK -> RingSegment.REWORK
    else -> RingSegment.TODO
}

@Composable
private fun ObjectiveUi.statusLabel(): Pair<String, Color> = when (status) {
    ExerciseStatus.VALIDATED -> stringResource(Res.string.status_validated) to Palette.Success
    ExerciseStatus.SUBMITTED -> stringResource(Res.string.status_submitted) to Palette.Blue
    ExerciseStatus.TO_REWORK -> stringResource(Res.string.status_to_rework) to BrandColors.Amber
    else -> stringResource(Res.string.progress_objective_todo) to Palette.Mist
}

/**
 * The objectives of the next level as cards to swipe, the ones still open first. A tap opens the
 * objective's sheet.
 */
@Composable
internal fun ObjectiveCarousel(objectives: List<ObjectiveUi>, modules: List<ModuleProgressUi>, onOpen: (ObjectiveUi) -> Unit) {
    val numbered = objectives.mapIndexed { i, o -> i + 1 to o }
        .sortedBy { (_, o) -> if (o.status == ExerciseStatus.VALIDATED) 1 else 0 }
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.l),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(numbered, key = { _, (_, o) -> o.id }) { order, (number, objective) ->
            val (label, color) = objective.statusLabel()
            val module = modules.firstOrNull { it.objectiveId == objective.id }
            val shape = RoundedCornerShape(22.dp)
            Column(
                Modifier
                    .enter(order)
                    .width(156.dp)
                    .height(172.dp)
                    .clip(shape)
                    .background(Palette.NightRaised)
                    .border(1.dp, if (objective.status == null) Palette.Outline else color.copy(alpha = 0.45f), shape)
                    .clickable(role = Role.Button) { onOpen(objective) }
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                StatusChip(label, color)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(Res.string.progress_objective_number, number), style = MaterialTheme.typography.labelMedium, color = Palette.Mist)
                    Text(objective.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                    if (module != null && module.done > 0 && objective.status != ExerciseStatus.VALIDATED) {
                        Text(stringResource(Res.string.progress_objective_module, module.done, module.total), style = MaterialTheme.typography.labelSmall, color = Palette.Blue)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.16f)).padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/**
 * Everything about one objective: what it teaches, what the super admin checks, the answer if it
 * came back, and the way forward: the guided module, the tick, or the microphone tier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ObjectiveSheet(
    objective: ObjectiveUi,
    number: Int,
    moduleId: String?,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onLearn: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
) {
    // Opened whole, clear of the navigation bar and scrollable: with a large font the tick and the
    // main button used to sit under the system buttons or below the half-open sheet.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Palette.NightRaised,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val (label, color) = objective.statusLabel()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                StatusChip(label, color)
                Text(stringResource(Res.string.progress_objective_number, number), style = MaterialTheme.typography.labelMedium, color = Palette.Mist)
            }
            Text(objective.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.growth_learn_title).uppercase(), style = MaterialTheme.typography.labelMedium, color = Palette.Violet)
                Text(objective.learn, style = MaterialTheme.typography.bodyLarge)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.NightHigh).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(Res.string.progress_sheet_criterion).uppercase(), style = MaterialTheme.typography.labelMedium, color = Palette.Mist)
                Text(objective.criterion, style = MaterialTheme.typography.bodyLarge)
            }
            if (objective.status == ExerciseStatus.TO_REWORK && objective.feedback != null) {
                Text("« ${objective.feedback} »", style = MaterialTheme.typography.bodyMedium, color = BrandColors.Amber)
            }
            if (moduleId != null && objective.status != ExerciseStatus.VALIDATED) {
                GradientButton(onClick = { onLearn(moduleId) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.progress_sheet_learn), style = MaterialTheme.typography.titleMedium)
                }
            }
            when {
                objective.autoTier != null -> Text(
                    stringResource(Res.string.growth_by_microphone, objective.autoTier),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Mist,
                )
                objective.status != ExerciseStatus.VALIDATED -> {
                    val ticked = objective.status == ExerciseStatus.SUBMITTED
                    // The whole row, explanation included, toggles: a 48 dp target at least.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .toggleable(value = ticked, enabled = !isWorking, role = Role.Checkbox) { onToggle(objective.id, it) }
                            .padding(vertical = Spacing.s),
                    ) {
                        Checkbox(checked = ticked, onCheckedChange = null, enabled = !isWorking)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(Res.string.progress_sheet_reached), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(Res.string.progress_sheet_reached_hint), style = MaterialTheme.typography.bodySmall, color = Palette.Mist)
                        }
                    }
                }
            }
        }
    }
}

/** The guided modules of the member's part, each with its progress bar. */
@Composable
internal fun ModulesList(modules: List<ModuleProgressUi>, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        modules.forEachIndexed { i, module ->
            val shape = RoundedCornerShape(20.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .enter(i)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Palette.NightRaised)
                    .border(1.dp, Palette.Outline, shape)
                    .clickable(role = Role.Button) { onOpen(module.id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(module.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    ProgressBar(if (module.total == 0) 0f else module.done.toFloat() / module.total)
                }
                Text(
                    stringResource(Res.string.progress_module_count, module.done, module.total),
                    style = MaterialTheme.typography.labelLarge,
                    color = Palette.Mist,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    // Fills from empty the first time, then follows the progress.
    val shown = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        delay(300)
        shown.animateTo(fraction, tween(800, easing = FastOutSlowInEasing))
    }
    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Palette.NightHigh)) {
        Box(
            Modifier.fillMaxWidth(shown.value).height(6.dp).clip(CircleShape).background(BrandColors.horizontalGradient),
        )
    }
}
