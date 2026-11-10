package com.xperiencemusic.shared.ui.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.presentation.progress.FocusUi
import com.xperiencemusic.shared.presentation.progress.LevelCardUi
import com.xperiencemusic.shared.presentation.progress.ModuleProgressUi
import com.xperiencemusic.shared.presentation.progress.ObjectiveUi
import com.xperiencemusic.shared.presentation.progress.focus
import com.xperiencemusic.shared.presentation.progress.startedObjectives
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.growth_ask
import com.xperiencemusic.shared.resources.growth_ask_again
import com.xperiencemusic.shared.resources.growth_objectives
import com.xperiencemusic.shared.resources.growth_rework
import com.xperiencemusic.shared.resources.growth_top
import com.xperiencemusic.shared.resources.growth_waiting
import com.xperiencemusic.shared.resources.progress_focus_continue
import com.xperiencemusic.shared.resources.progress_focus_done_eyebrow
import com.xperiencemusic.shared.resources.progress_focus_done_text
import com.xperiencemusic.shared.resources.progress_focus_done_title
import com.xperiencemusic.shared.resources.progress_focus_learning_eyebrow
import com.xperiencemusic.shared.resources.progress_focus_learning_text
import com.xperiencemusic.shared.resources.progress_focus_rework_cta
import com.xperiencemusic.shared.resources.progress_focus_rework_eyebrow
import com.xperiencemusic.shared.resources.progress_focus_rework_text
import com.xperiencemusic.shared.resources.progress_focus_start
import com.xperiencemusic.shared.resources.progress_focus_step_eyebrow
import com.xperiencemusic.shared.resources.progress_focus_voice_cta
import com.xperiencemusic.shared.resources.progress_focus_voice_eyebrow
import com.xperiencemusic.shared.resources.progress_focus_voice_text
import com.xperiencemusic.shared.resources.progress_focus_warmup_eyebrow
import com.xperiencemusic.shared.resources.progress_focus_warmup_text
import com.xperiencemusic.shared.resources.progress_ring_top
import com.xperiencemusic.shared.resources.progress_ring_towards
import com.xperiencemusic.shared.resources.progress_warmup_title
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.AppTypography
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** The ring of the next level, the count inside, and the request to move up underneath. */
@Composable
internal fun LevelHero(
    card: LevelCardUi,
    objectives: List<ObjectiveUi>,
    modules: List<ModuleProgressUi>,
    isWorking: Boolean,
    calm: Boolean,
    modifier: Modifier = Modifier,
    onAsk: () -> Unit,
) {
    val next = card.next
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (next == null) {
            LevelRing(List(objectives.size.coerceAtLeast(1)) { RingSegment.VALIDATED }, stringResource(Res.string.growth_top), calm = calm) {
                Text(stringResource(Res.string.progress_ring_top), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            }
            Text(stringResource(Res.string.growth_top), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.success)
            return@Column
        }
        val started = startedObjectives(objectives, modules)
        val segments = objectives.map { it.segment(it.id in started) }.ifEmpty { List(card.required) { if (it < card.done) RingSegment.VALIDATED else RingSegment.TODO } }
        LevelRing(segments, stringResource(Res.string.growth_objectives, card.done, card.required, next.target()), calm = calm) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    CountUp(card.done, MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold))
                    Text("/${card.required}", style = AppTypography.numberSuffix, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = Spacing.s))
                }
                Text(stringResource(Res.string.progress_ring_towards, next.target()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            card.requestStatus == ExerciseStatus.SUBMITTED ->
                Text(stringResource(Res.string.growth_waiting), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.info, modifier = Modifier.padding(horizontal = Spacing.l))
            card.requestStatus == ExerciseStatus.TO_REWORK && card.feedback != null ->
                Text(stringResource(Res.string.growth_rework, card.feedback), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.warning, modifier = Modifier.padding(horizontal = Spacing.l))
        }
        if (card.canAsk) {
            GradientButton(onClick = onAsk, enabled = !isWorking, modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s)) {
                Text(
                    stringResource(if (card.requestStatus == ExerciseStatus.TO_REWORK) Res.string.growth_ask_again else Res.string.growth_ask, next.target()),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/** Today's single action, worded for what [focus] is. */
@Composable
internal fun TodayCard(
    focus: FocusUi,
    onExerciseClick: (String) -> Unit,
    onOpenLearning: (String) -> Unit,
    onRecordVoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    @Composable
    fun cta(label: String, onClick: () -> Unit): @Composable () -> Unit = {
        GradientButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
    when (focus) {
        is FocusUi.ReworkVoice -> FocusCard(
            stringResource(Res.string.progress_focus_rework_eyebrow), focus.title,
            focus.feedback?.let { "« $it »" } ?: stringResource(Res.string.progress_focus_rework_text), modifier,
            cta(stringResource(Res.string.progress_focus_rework_cta)) { onRecordVoice(focus.songId) },
        )
        is FocusUi.Step -> FocusCard(
            stringResource(Res.string.progress_focus_step_eyebrow), focus.title,
            listOf(stringResource(tierTitles.getOrElse(focus.tier - 1) { tierTitles.last() }), focus.detail).filter { it.isNotBlank() }.joinToString(" · "),
            modifier,
            cta(stringResource(Res.string.progress_focus_continue)) { onExerciseClick(focus.stepId) },
        )
        is FocusUi.Learning -> FocusCard(
            stringResource(Res.string.progress_focus_learning_eyebrow), focus.moduleTitle,
            stringResource(Res.string.progress_focus_learning_text, focus.activityTitle, focus.done, focus.total), modifier,
            cta(stringResource(if (focus.done == 0) Res.string.progress_focus_start else Res.string.progress_focus_continue)) { onOpenLearning(focus.moduleId) },
        )
        is FocusUi.SendVoice -> FocusCard(
            stringResource(Res.string.progress_focus_voice_eyebrow), focus.title,
            stringResource(Res.string.progress_focus_voice_text), modifier,
            cta(stringResource(Res.string.progress_focus_voice_cta)) { onRecordVoice(focus.songId) },
        )
        is FocusUi.WarmUp -> FocusCard(
            stringResource(Res.string.progress_focus_warmup_eyebrow), stringResource(Res.string.progress_warmup_title),
            stringResource(Res.string.progress_focus_warmup_text), modifier,
            cta(stringResource(Res.string.progress_focus_continue)) { onExerciseClick(focus.id) },
        )
        FocusUi.UpToDate -> FocusCard(
            stringResource(Res.string.progress_focus_done_eyebrow), stringResource(Res.string.progress_focus_done_title),
            stringResource(Res.string.progress_focus_done_text), modifier,
        )
    }
}
