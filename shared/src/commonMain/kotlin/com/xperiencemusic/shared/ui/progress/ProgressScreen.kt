package com.xperiencemusic.shared.ui.progress

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.StepState
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.progress.LevelCardUi
import com.xperiencemusic.shared.presentation.progress.ObjectiveUi
import com.xperiencemusic.shared.presentation.progress.ProgressEvent
import com.xperiencemusic.shared.presentation.progress.ProgressUiState
import com.xperiencemusic.shared.presentation.progress.ProgressViewModel
import com.xperiencemusic.shared.presentation.progress.StepUi
import com.xperiencemusic.shared.presentation.progress.TierUi
import com.xperiencemusic.shared.presentation.progress.WarmUpUi
import com.xperiencemusic.shared.presentation.progress.focus
import com.xperiencemusic.shared.presentation.progress.learningModules
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.progress_musicians_soon_text
import com.xperiencemusic.shared.resources.progress_musicians_soon_title
import com.xperiencemusic.shared.resources.progress_tab_objectives
import com.xperiencemusic.shared.resources.progress_tab_path
import com.xperiencemusic.shared.resources.progress_tab_voice
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.xperiencemusic.shared.ui.components.LoadingIndicator
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition

/** The violet glow behind the top of the screen, lifted so that it fades out under the ring. */
private val GLOW_SIZE = 520.dp
private val GLOW_LIFT = 160.dp

@Composable
fun ProgressScreen(
    onExerciseClick: (String) -> Unit,
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
        onRecordVoice = onRecordVoice,
        onReview = onReview,
    )
}

/** The sections under the focus card; only those with something to show appear. */
private enum class Section { OBJECTIVES, PATH, VOICE }

/**
 * One thing at a time: who you are and your streak, the ring of the next level, today's single
 * action, then the details behind three tabs. Admin tools live in "Mon compte" and a small strip.
 */
@Composable
fun ProgressScreenContent(
    state: ProgressUiState,
    onExerciseClick: (String) -> Unit,
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
        LoadingIndicator()
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

    CompositionLocalProvider(LocalCalmMotion provides state.reduceMotion) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .size(GLOW_SIZE)
                    .padding(bottom = GLOW_LIFT)
                    .background(Brush.radialGradient(listOf(BrandColors.Indigo.copy(alpha = 0.22f), MaterialTheme.colorScheme.background.copy(alpha = 0f)))),
            )
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Spacing.l)) {
                ProgressHeader(state, Modifier.padding(horizontal = Spacing.l).enter(0))
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
                            if (state.reduceMotion) return@AnimatedContent EnterTransition.None togetherWith ExitTransition.None
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
                    ObjectiveUi("singer-c${i + 1}", title, "Ce qu’on apprend", "Le critère", if (i < 2) ExerciseStatus.VALIDATED else null, null)
                },
                tiers = listOf(
                    TierUi(
                        1,
                        isComplete = false,
                        steps = listOf(
                            step("a", "Tenir 5 secondes", StepState.DONE),
                            step("b", "Tenir 10 secondes", StepState.DONE),
                            step("c", "Souffle long 15 s", StepState.CURRENT),
                            step("d", "Défi · l’escalier", StepState.LOCKED, challenge = true),
                        ),
                    ),
                ),
            ),
            onExerciseClick = {},
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
        )
    }
}
