package com.xperiencemusic.shared.ui.progress

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.StepState
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.progress.FocusUi
import com.xperiencemusic.shared.presentation.progress.LevelCardUi
import com.xperiencemusic.shared.presentation.progress.ModuleProgressUi
import com.xperiencemusic.shared.presentation.progress.ObjectiveUi
import com.xperiencemusic.shared.presentation.progress.ProgressEvent
import com.xperiencemusic.shared.presentation.progress.ProgressUiState
import com.xperiencemusic.shared.presentation.progress.ProgressViewModel
import com.xperiencemusic.shared.presentation.progress.StepUi
import com.xperiencemusic.shared.presentation.progress.TierUi
import com.xperiencemusic.shared.presentation.progress.WarmUpUi
import com.xperiencemusic.shared.presentation.progress.WeekVoiceUi
import com.xperiencemusic.shared.presentation.progress.focus
import com.xperiencemusic.shared.presentation.progress.learningModules
import com.xperiencemusic.shared.presentation.progress.startedObjectives
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.growth_ask
import com.xperiencemusic.shared.resources.growth_ask_again
import com.xperiencemusic.shared.resources.growth_objectives
import com.xperiencemusic.shared.resources.growth_rework
import com.xperiencemusic.shared.resources.growth_top
import com.xperiencemusic.shared.resources.growth_validate_open
import com.xperiencemusic.shared.resources.growth_waiting
import com.xperiencemusic.shared.resources.members_open
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
import com.xperiencemusic.shared.resources.progress_menu
import com.xperiencemusic.shared.resources.progress_modules_title
import com.xperiencemusic.shared.resources.progress_musicians_soon_text
import com.xperiencemusic.shared.resources.progress_musicians_soon_title
import com.xperiencemusic.shared.resources.progress_open_step
import com.xperiencemusic.shared.resources.progress_ring_top
import com.xperiencemusic.shared.resources.progress_ring_towards
import com.xperiencemusic.shared.resources.progress_streak
import com.xperiencemusic.shared.resources.progress_tab_objectives
import com.xperiencemusic.shared.resources.progress_tab_path
import com.xperiencemusic.shared.resources.progress_tab_voice
import com.xperiencemusic.shared.resources.progress_tier_1
import com.xperiencemusic.shared.resources.progress_tier_2
import com.xperiencemusic.shared.resources.progress_tier_3
import com.xperiencemusic.shared.resources.progress_tier_4
import com.xperiencemusic.shared.resources.progress_tier_5
import com.xperiencemusic.shared.resources.progress_voice_empty
import com.xperiencemusic.shared.resources.progress_warmup_done
import com.xperiencemusic.shared.resources.progress_warmup_hint
import com.xperiencemusic.shared.resources.progress_warmup_title
import com.xperiencemusic.shared.resources.review_open
import com.xperiencemusic.shared.resources.session_sign_out
import com.xperiencemusic.shared.resources.tab_progress
import com.xperiencemusic.shared.resources.voice_week_none
import com.xperiencemusic.shared.resources.voice_week_title
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.brand.TabHeader
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Palette
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
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

/** The sections under the focus card; only those with something to show appear. */
private enum class Section { OBJECTIVES, PATH, VOICE }

/**
 * One thing at a time: who you are and your streak, the ring of the next level, today's single
 * action, then the details behind three tabs. Admin tools live in the menu and a small strip.
 */
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
    val modules = remember(state.learning, state.instrument) { state.learningModules() }
    val sections = buildList {
        if (state.objectives.isNotEmpty()) add(Section.OBJECTIVES)
        if (state.hasPath || modules.isNotEmpty()) add(Section.PATH)
        if (state.hasPath) add(Section.VOICE)
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val section = sections.getOrNull(tab) ?: sections.firstOrNull()
    var sheetId by rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(520.dp)
                .padding(bottom = 160.dp)
                .background(Brush.radialGradient(listOf(BrandColors.Indigo.copy(alpha = 0.22f), Palette.Night.copy(alpha = 0f)))),
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Spacing.l)) {
            Header(state, onReview, onValidate, onManageMembers, onSignOut, Modifier.padding(horizontal = Spacing.l).enter(0))
            if (state.canReview && (state.toReviewCount > 0 || state.toValidateCount > 0)) {
                AdminStrip(state, onReview, onValidate, Modifier.padding(start = Spacing.l, end = Spacing.l, top = Spacing.m).enter(1))
            }
            state.levelCard?.let { card ->
                LevelHero(card, state.objectives, modules, state.isWorking, state.reduceMotion, Modifier.fillMaxWidth().padding(top = Spacing.s).enter(1)) {
                    onEvent(ProgressEvent.AskLevelUpClicked)
                }
            }
            TodayCard(state.focus(), onExerciseClick, onOpenLearning, onRecordVoice, Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m).enter(2))
            if (sections.size > 1 && section != null) {
                SectionTabs(
                    labels = sections.map { stringResource(it.label()) },
                    selected = sections.indexOf(section),
                    onSelect = { tab = it },
                    modifier = Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.m).enter(3),
                )
            }
            if (section != null) {
                AnimatedContent(
                    targetState = section,
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally { it / 5 * direction } + fadeIn())
                            .togetherWith(slideOutHorizontally { -it / 5 * direction } + fadeOut())
                            .using(SizeTransform(clip = false))
                    },
                    label = "section",
                ) { shown ->
                    when (shown) {
                        Section.OBJECTIVES -> ObjectiveList(state.objectives, modules) { sheetId = it.id }
                        Section.PATH -> PathSection(state, modules, onExerciseClick, onOpenLearning)
                        Section.VOICE -> VoiceSection(state.weekVoices, onRecordVoice)
                    }
                }
            } else if (state.levelCard?.next != null) {
                EmptyState(
                    icon = XmIcons.Piano,
                    title = stringResource(Res.string.progress_musicians_soon_title),
                    text = stringResource(Res.string.progress_musicians_soon_text),
                    modifier = Modifier.padding(horizontal = Spacing.l),
                )
            }
            Spacer(Modifier.height(Spacing.xl))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    val open = state.objectives.indexOfFirst { it.id == sheetId }
    if (open >= 0) {
        val objective = state.objectives[open]
        ObjectiveSheet(
            objective = objective,
            number = open + 1,
            moduleId = modules.firstOrNull { it.objectiveId == objective.id }?.id,
            isWorking = state.isWorking,
            onDismiss = { sheetId = null },
            onLearn = { sheetId = null; onOpenLearning(it) },
            onToggle = { id, reached -> onEvent(ProgressEvent.ObjectiveToggled(id, reached)) },
        )
    }
}

private fun Section.label(): StringResource = when (this) {
    Section.OBJECTIVES -> Res.string.progress_tab_objectives
    Section.PATH -> Res.string.progress_tab_path
    Section.VOICE -> Res.string.progress_tab_voice
}

@Composable
private fun Header(
    state: ProgressUiState,
    onReview: () -> Unit,
    onValidate: () -> Unit,
    onManageMembers: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    TabHeader(
        title = stringResource(Res.string.tab_progress),
        subtitle = listOfNotNull(state.name.takeIf { it.isNotBlank() }, state.voicePart?.label() ?: state.instrument?.label(), state.level?.label()).joinToString(" · "),
        modifier = modifier,
    ) {
        if (state.hasPath) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Palette.NightRaised)
                    .border(1.dp, Palette.Outline, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Icon(XmIcons.Flame, contentDescription = null, tint = if (state.streakDays > 0) BrandColors.Amber else Palette.Mist, modifier = Modifier.size(16.dp))
                Text(pluralStringResource(Res.plurals.progress_streak, state.streakDays, state.streakDays), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
            }
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.progress_menu))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (state.canReview) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.review_open, state.toReviewCount)) }, onClick = { menu = false; onReview() })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.growth_validate_open, state.toValidateCount)) }, onClick = { menu = false; onValidate() })
                }
                if (state.canManageMembers) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.members_open)) }, onClick = { menu = false; onManageMembers() })
                }
                DropdownMenuItem(text = { Text(stringResource(Res.string.session_sign_out)) }, onClick = { menu = false; onSignOut() })
            }
        }
    }
}

/** For the Admin++: what waits for them, only when something does. */
@Composable
private fun AdminStrip(state: ProgressUiState, onReview: () -> Unit, onValidate: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        if (state.toReviewCount > 0) AdminPill(stringResource(Res.string.review_open, state.toReviewCount), onReview)
        if (state.toValidateCount > 0) AdminPill(stringResource(Res.string.growth_validate_open, state.toValidateCount), onValidate)
    }
}

@Composable
private fun AdminPill(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .border(1.dp, BrandColors.gradient, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** The ring of the next level, the count inside, and the request to move up underneath. */
@Composable
private fun LevelHero(
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
            Text(stringResource(Res.string.growth_top), style = MaterialTheme.typography.bodyMedium, color = Palette.Success)
            return@Column
        }
        val started = startedObjectives(objectives, modules)
        val segments = objectives.map { it.segment(it.id in started) }.ifEmpty { List(card.required) { if (it < card.done) RingSegment.VALIDATED else RingSegment.TODO } }
        LevelRing(segments, stringResource(Res.string.growth_objectives, card.done, card.required, next.target()), calm = calm) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    CountUp(card.done, MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold))
                    Text("/${card.required}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Palette.Mist, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text(stringResource(Res.string.progress_ring_towards, next.target()), style = MaterialTheme.typography.labelLarge, color = Palette.Mist)
            }
        }
        when {
            card.requestStatus == ExerciseStatus.SUBMITTED ->
                Text(stringResource(Res.string.growth_waiting), style = MaterialTheme.typography.bodyMedium, color = Palette.Blue, modifier = Modifier.padding(horizontal = Spacing.l))
            card.requestStatus == ExerciseStatus.TO_REWORK && card.feedback != null ->
                Text(stringResource(Res.string.growth_rework, card.feedback), style = MaterialTheme.typography.bodyMedium, color = BrandColors.Amber, modifier = Modifier.padding(horizontal = Spacing.l))
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
private fun TodayCard(
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

/** The warm-up, the guided modules, then the singers' microphone path. */
@Composable
private fun PathSection(
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
private fun VoiceSection(voices: List<WeekVoiceUi>, onRecordVoice: (String) -> Unit) {
    Column(Modifier.padding(horizontal = Spacing.l)) {
        if (voices.isEmpty()) {
            Text(stringResource(Res.string.progress_voice_empty), style = MaterialTheme.typography.bodyMedium, color = Palette.Mist)
        } else {
            WeekVoicesCard(voices, onRecordVoice)
        }
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
                level = Level.BEGINNER,
                voicePart = VoicePart.ALTO,
                hasPath = true,
                streakDays = 5,
                completedTiers = 0,
                warmUp = WarmUpUi("warmup", isDoneToday = false),
                levelCard = LevelCardUi(Level.BEGINNER, Level.CONFIRMED, done = 2, required = 7, requestStatus = null, feedback = null, canAsk = false),
                objectives = listOf("Souffle et posture", "Justesse", "Intervalles", "Rythme", "Micro", "Tenir sa voix", "Présence").mapIndexed { i, title ->
                    ObjectiveUi("singer-c${i + 1}", title, "Ce qu'on apprend", "Le critère", if (i < 2) ExerciseStatus.VALIDATED else null, null)
                },
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
