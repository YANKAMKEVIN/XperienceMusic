package com.xperiencemusic.shared.ui.progress

import com.xperiencemusic.shared.presentation.progress.ProgressEvent
import com.xperiencemusic.shared.resources.growth_validate_open
import com.xperiencemusic.shared.resources.error_generic
import org.jetbrains.compose.resources.getString
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.StepState
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.progress.ProgressUiState
import com.xperiencemusic.shared.presentation.progress.ProgressViewModel
import com.xperiencemusic.shared.presentation.progress.StepUi
import com.xperiencemusic.shared.presentation.progress.TierUi
import com.xperiencemusic.shared.presentation.progress.WarmUpUi
import com.xperiencemusic.shared.presentation.progress.WeekVoiceUi
import com.xperiencemusic.shared.resources.review_open
import com.xperiencemusic.shared.resources.voice_week_none
import com.xperiencemusic.shared.resources.voice_week_title
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.members_open
import com.xperiencemusic.shared.resources.progress_hello
import com.xperiencemusic.shared.resources.progress_medals
import com.xperiencemusic.shared.resources.progress_musicians_soon_text
import com.xperiencemusic.shared.resources.progress_musicians_soon_title
import com.xperiencemusic.shared.resources.progress_open_step
import com.xperiencemusic.shared.resources.progress_streak
import com.xperiencemusic.shared.resources.progress_tier_1
import com.xperiencemusic.shared.resources.progress_tier_2
import com.xperiencemusic.shared.resources.progress_tier_3
import com.xperiencemusic.shared.resources.progress_tier_4
import com.xperiencemusic.shared.resources.progress_tier_5
import com.xperiencemusic.shared.resources.progress_title
import com.xperiencemusic.shared.resources.progress_warmup_done
import com.xperiencemusic.shared.resources.progress_warmup_hint
import com.xperiencemusic.shared.resources.progress_warmup_title
import com.xperiencemusic.shared.resources.session_sign_out
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProgressScreen(
    onExerciseClick: (String) -> Unit,
    onManageMembers: () -> Unit,
    onSignOut: () -> Unit,
    onRecordVoice: (String) -> Unit = {},
    onReview: () -> Unit = {},
    onValidate: () -> Unit = {},
    onOpenLearning: (String) -> Unit = {},
    viewModel: ProgressViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgressScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onValidate = onValidate,
        onOpenLearning = onOpenLearning,
        onExerciseClick = onExerciseClick,
        onManageMembers = onManageMembers,
        onSignOut = onSignOut,
        onRecordVoice = onRecordVoice,
        onReview = onReview,
    )
}

private val tierTitles: List<StringResource> = listOf(
    Res.string.progress_tier_1,
    Res.string.progress_tier_2,
    Res.string.progress_tier_3,
    Res.string.progress_tier_4,
    Res.string.progress_tier_5,
)

@Composable
fun ProgressScreenContent(
    state: ProgressUiState,
    onExerciseClick: (String) -> Unit,
    onManageMembers: () -> Unit,
    onSignOut: () -> Unit,
    onRecordVoice: (String) -> Unit = {},
    onReview: () -> Unit = {},
    onEvent: (ProgressEvent) -> Unit = {},
    onValidate: () -> Unit = {},
    onOpenLearning: (String) -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.hasError) {
        if (state.hasError) {
            snackbar.showSnackbar(getString(Res.string.error_generic))
            onEvent(ProgressEvent.ErrorShown)
        }
    }
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val openLabel = stringResource(Res.string.progress_open_step)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            item { ProgressHeader(state) }
            state.learning?.let { course ->
                item {
                    AppCard {
                        Text("PARCOURS PIANO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Accords de base", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = Spacing.s))
                        Text("${course.completed.size} / ${com.xperiencemusic.shared.domain.learning.PianoBasics.activities.size} activités terminées · compétence évaluée par le responsable", modifier = Modifier.padding(top = Spacing.s))
                        Text(com.xperiencemusic.shared.domain.learning.PianoBasics.activities[course.nextIndex].title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.s))
                        GradientButton(onClick = { onOpenLearning("piano-chords") }, modifier = Modifier.padding(top = Spacing.m)) {
                            Text(if (course.isComplete) "Réviser et voir mon évaluation" else if (course.completed.isEmpty()) "Commencer mon apprentissage" else "Continuer mon apprentissage")
                        }
                    }
                }
            }
            state.levelCard?.let { card ->
                item { LevelCard(card, state.isWorking) { onEvent(ProgressEvent.AskLevelUpClicked) } }
            }
            val next = state.levelCard?.next
            if (next != null && state.objectives.isNotEmpty()) {
                item {
                    ObjectivesCard(next, state.objectives, state.isWorking, onLearn = if (state.learning != null) onOpenLearning else null) { id, reached ->
                        onEvent(ProgressEvent.ObjectiveToggled(id, reached))
                    }
                }
            }
            if (!state.hasPath && next != null && state.objectives.isEmpty()) {
                item {
                    EmptyState(
                        icon = XmIcons.Piano,
                        title = stringResource(Res.string.progress_musicians_soon_title),
                        text = stringResource(Res.string.progress_musicians_soon_text),
                    )
                }
            } else if (state.hasPath) {
                state.warmUp?.let { warmUp -> item { WarmUpCard(warmUp, onClick = { onExerciseClick(warmUp.id) }) } }
                if (state.weekVoices.isNotEmpty()) item { WeekVoicesCard(state.weekVoices, onRecordVoice) }
                // Each tier picks up the winding line where the previous one stopped.
                var lane = 0
                state.tiers.forEach { tier ->
                    val firstLane = lane
                    lane += tier.steps.size
                    item(key = "tier-${tier.number}") {
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
            if (state.canReview) {
                item {
                    GradientButton(onClick = onReview, modifier = Modifier.fillMaxWidth().padding(top = Spacing.l)) {
                        Icon(XmIcons.Mic, contentDescription = null, modifier = Modifier.padding(end = Spacing.s).size(20.dp))
                        Text(stringResource(Res.string.review_open, state.toReviewCount), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            if (state.canReview) {
                item {
                    OutlinedButton(onClick = onValidate, modifier = Modifier.fillMaxWidth().padding(top = Spacing.s)) {
                        Icon(XmIcons.Medal, contentDescription = null, modifier = Modifier.padding(end = Spacing.s).size(20.dp))
                        Text(stringResource(Res.string.growth_validate_open, state.toValidateCount))
                    }
                }
            }
            if (state.canManageMembers) {
                item {
                    OutlinedButton(onClick = onManageMembers, modifier = Modifier.fillMaxWidth().padding(top = Spacing.l)) {
                        Text(stringResource(Res.string.members_open))
                    }
                }
            }
            item {
                TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().padding(top = Spacing.l)) {
                    Text(stringResource(Res.string.session_sign_out))
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ProgressHeader(state: ProgressUiState) {
    Column {
        Text(
            stringResource(Res.string.progress_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            listOfNotNull(
                stringResource(Res.string.progress_hello, state.name),
                state.voicePart?.label() ?: state.instrument?.label(),
                state.level?.label(),
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.hasPath) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(top = Spacing.m)) {
                StatChip(XmIcons.Flame, stringResource(Res.string.progress_streak, state.streakDays), highlighted = state.streakDays > 0)
                StatChip(XmIcons.Medal, stringResource(Res.string.progress_medals, state.completedTiers), highlighted = state.completedTiers > 0)
            }
        }
    }
}

@Composable
private fun StatChip(icon: ImageVector, text: String, highlighted: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, if (highlighted) colors.primary else colors.outlineVariant, CircleShape)
            .padding(horizontal = Spacing.m, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (highlighted) BrandColors.Purple else colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
    }
}

/** The daily warm-up: three minutes that keep the streak going. */
@Composable
private fun WarmUpCard(warmUp: WarmUpUi, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainer)
            .border(1.5.dp, if (warmUp.isDoneToday) SolidColor(colors.outlineVariant) else BrandColors.gradient, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Spacing.m),
    ) {
        Box(
            Modifier.size(44.dp).background(colors.primaryContainer, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(XmIcons.Mic, contentDescription = null, tint = BrandColors.Purple, modifier = Modifier.size(24.dp))
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
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { onRecordVoice(voice.songId) }
                    .padding(vertical = Spacing.s),
            ) {
                Icon(XmIcons.Mic, contentDescription = null, tint = BrandColors.Purple, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
                    Text(voice.title, style = MaterialTheme.typography.titleSmall)
                    voice.feedback?.let {
                        Text("« $it »", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Preview
@Composable
private fun ProgressScreenSingerPreview() {
    fun step(id: String, title: String, state: StepState, challenge: Boolean = false) =
        StepUi(id, title, "Ré4 · 5 s", state, challenge)
    AppTheme {
        ProgressScreenContent(
            ProgressUiState(
                isLoading = false,
                name = "Sarah",
                instrument = Instrument.SINGER,
                level = Level.CONFIRMED,
                voicePart = VoicePart.ALTO,
                hasPath = true,
                streakDays = 5,
                completedTiers = 0,
                warmUp = WarmUpUi("warmup", isDoneToday = false),
                tiers = listOf(
                    TierUi(
                        1,
                        isComplete = false,
                        steps = listOf(
                            step("a", "Tenir 5 secondes", StepState.DONE),
                            step("b", "Tenir 10 secondes", StepState.DONE),
                            step("c", "Souffle long 15 s", StepState.CURRENT),
                            step("d", "Défi · l'escalier", StepState.LOCKED, challenge = true),
                        ),
                    ),
                ),
            ),
            onExerciseClick = {},
            onManageMembers = {},
            onSignOut = {},
        )
    }
}

@Preview
@Composable
private fun ProgressScreenMusicianPreview() {
    AppTheme {
        ProgressScreenContent(
            ProgressUiState(isLoading = false, name = "Daniel", instrument = Instrument.PIANO, level = Level.PRO),
            onExerciseClick = {},
            onManageMembers = {},
            onSignOut = {},
        )
    }
}
