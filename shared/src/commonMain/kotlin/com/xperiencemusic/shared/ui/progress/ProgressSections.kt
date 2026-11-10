package com.xperiencemusic.shared.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.xperiencemusic.shared.presentation.progress.ModuleProgressUi
import com.xperiencemusic.shared.presentation.progress.ProgressUiState
import com.xperiencemusic.shared.presentation.progress.WarmUpUi
import com.xperiencemusic.shared.presentation.progress.WeekVoiceUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.progress_modules_title
import com.xperiencemusic.shared.resources.progress_open_step
import com.xperiencemusic.shared.resources.progress_tier_1
import com.xperiencemusic.shared.resources.progress_tier_2
import com.xperiencemusic.shared.resources.progress_tier_3
import com.xperiencemusic.shared.resources.progress_tier_4
import com.xperiencemusic.shared.resources.progress_tier_5
import com.xperiencemusic.shared.resources.progress_voice_empty
import com.xperiencemusic.shared.resources.progress_warmup_done
import com.xperiencemusic.shared.resources.progress_warmup_hint
import com.xperiencemusic.shared.resources.progress_warmup_title
import com.xperiencemusic.shared.resources.quoted_feedback
import com.xperiencemusic.shared.resources.voice_week_none
import com.xperiencemusic.shared.resources.voice_week_title
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Radius
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal val tierTitles: List<StringResource> = listOf(
    Res.string.progress_tier_1,
    Res.string.progress_tier_2,
    Res.string.progress_tier_3,
    Res.string.progress_tier_4,
    Res.string.progress_tier_5,
)

/** The warm-up, the guided modules, then the singers' microphone path. */
@Composable
internal fun PathSection(
    state: ProgressUiState,
    modules: List<com.xperiencemusic.shared.presentation.progress.ModuleProgressUi>,
    onExerciseClick: (String) -> Unit,
    onOpenLearning: (String) -> Unit,
) {
    val openLabel = stringResource(Res.string.progress_open_step)
    Column(Modifier.padding(horizontal = Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        state.warmUp?.let { WarmUpCard(it, onClick = { onExerciseClick(it.id) }) }
        if (modules.isNotEmpty()) {
            Text(stringResource(Res.string.progress_modules_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ModulesList(modules, onOpenLearning)
        }
        // Each tier picks up the winding line where the previous one stopped.
        var lane = 0
        state.tiers.forEach { tier ->
            val firstLane = lane
            lane += tier.steps.size
            TierPath(
                tier = tier,
                title = stringResource(tierTitles.getOrElse(tier.number - 1) { tierTitles.last() }),
                firstLane = firstLane,
                labelOpen = openLabel,
                onStepClick = onExerciseClick,
            )
        }
    }
}

@Composable
internal fun VoiceSection(voices: List<WeekVoiceUi>, onRecordVoice: (String) -> Unit) {
    Column(Modifier.padding(horizontal = Spacing.l)) {
        if (voices.isEmpty()) {
            Text(stringResource(Res.string.progress_voice_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            WeekVoicesCard(voices, onRecordVoice)
        }
    }
}

/** The daily warm-up: three minutes that keep the streak going. */
@Composable
private fun WarmUpCard(warmUp: WarmUpUi, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.cardLarge)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainer)
            .border(Sizes.strokeMedium, if (warmUp.isDoneToday) SolidColor(colors.outlineVariant) else BrandColors.gradient, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Spacing.m),
    ) {
        Box(
            Modifier.size(Sizes.iconTile).background(colors.primaryContainer, RoundedCornerShape(Radius.chip)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(XmIcons.Mic, contentDescription = null, tint = BrandColors.Purple, modifier = Modifier.size(Sizes.icon))
        }
        Column(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
            Text(stringResource(Res.string.progress_warmup_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (warmUp.isDoneToday) Res.string.progress_warmup_done else Res.string.progress_warmup_hint),
                style = MaterialTheme.typography.bodySmall,
                color = if (warmUp.isDoneToday) colors.tertiary else colors.onSurfaceVariant,
            )
        }
        Icon(
            if (warmUp.isDoneToday) Icons.Filled.Check else Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = if (warmUp.isDoneToday) colors.tertiary else colors.onSurface,
        )
    }
}

/** "Ta voix sur les chants de la semaine": one row per song, with what was sent and the answer. */
@Composable
private fun WeekVoicesCard(voices: List<WeekVoiceUi>, onRecordVoice: (String) -> Unit) {
    AppCard {
        Text(stringResource(Res.string.voice_week_title), style = MaterialTheme.typography.titleMedium)
        voices.forEach { voice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.s)
                    .clip(RoundedCornerShape(Radius.field))
                    .clickable(role = Role.Button) { onRecordVoice(voice.songId) }
                    .padding(vertical = Spacing.s),
            ) {
                Icon(XmIcons.Mic, contentDescription = null, tint = BrandColors.Purple, modifier = Modifier.size(Sizes.iconM))
                Column(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
                    Text(voice.title, style = MaterialTheme.typography.titleSmall)
                    voice.feedback?.let {
                        Text(stringResource(Res.string.quoted_feedback, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (voice.status != null) {
                    ExerciseStatusBadge(voice.status)
                } else {
                    Text(
                        stringResource(Res.string.voice_week_none),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
