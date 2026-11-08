package com.xperiencemusic.shared.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.ui.components.SystemBackHandler
import com.xperiencemusic.shared.data.remote.RemoteLoadMonitor
import com.xperiencemusic.shared.presentation.reminders.RemindersViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.tab_library
import com.xperiencemusic.shared.resources.tab_planning
import com.xperiencemusic.shared.resources.tab_progress
import com.xperiencemusic.shared.resources.tab_progress_short
import com.xperiencemusic.shared.resources.tab_week
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.exercise.ExerciseScreen
import com.xperiencemusic.shared.ui.library.LibraryScreen
import com.xperiencemusic.shared.ui.members.MembersScreen
import com.xperiencemusic.shared.ui.planning.AvailabilityScreen
import com.xperiencemusic.shared.ui.planning.PlanningScreen
import com.xperiencemusic.shared.ui.planning.ServiceDetailScreen
import com.xperiencemusic.shared.ui.platform.AskNotificationPermission
import com.xperiencemusic.shared.ui.player.MiniPlayer
import com.xperiencemusic.shared.ui.profile.ProfileScreen
import com.xperiencemusic.shared.ui.progress.GrowthReviewScreen
import com.xperiencemusic.shared.ui.progress.ProgressScreen
import com.xperiencemusic.shared.ui.recordingform.RecordingFormScreen
import com.xperiencemusic.shared.ui.setlisteditor.SetlistEditorScreen
import com.xperiencemusic.shared.ui.song.SongDetailScreen
import com.xperiencemusic.shared.ui.songeditor.SongEditorScreen
import com.xperiencemusic.shared.ui.stage.StageScreen
import com.xperiencemusic.shared.ui.theme.Palette
import com.xperiencemusic.shared.ui.theme.Spacing
import com.xperiencemusic.shared.ui.voice.VoiceRecordScreen
import com.xperiencemusic.shared.ui.voice.VoiceReviewScreen
import com.xperiencemusic.shared.ui.week.WeekScreen
import kotlin.reflect.KClass
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** [shortLabel] is what fits in the tab capsule; [label] is the full name read aloud. */
private data class Tab(
    val route: Any,
    val routeClass: KClass<*>,
    val label: StringResource,
    val icon: ImageVector,
    val shortLabel: StringResource = label,
)

private val tabs = listOf(
    Tab(WeekRoute, WeekRoute::class, Res.string.tab_week, XmIcons.Notes),
    Tab(LibraryRoute, LibraryRoute::class, Res.string.tab_library, XmIcons.Book),
    Tab(PlanningRoute, PlanningRoute::class, Res.string.tab_planning, XmIcons.Calendar),
    Tab(ProgressRoute, ProgressRoute::class, Res.string.tab_progress, XmIcons.Bars, Res.string.tab_progress_short),
)

/** The signed-in shell: four tabs, with the song detail pushed on top of them. */
@Composable
fun MainScaffold(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    // Keeps the phone's planning reminders in step for as long as someone is signed in.
    koinViewModel<RemindersViewModel>()
    AskNotificationPermission()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isOnTab = tabs.any { tab -> destination?.hasRoute(tab.routeClass) == true }
    // The stage takes the whole screen: no tabs, no mini player.
    val isOnStage = destination?.hasRoute(StageRoute::class) == true
    // Recording a voice: the guide plays in the earphones, a player card would only be in the way.
    val isRecordingVoice = destination?.hasRoute(VoiceRecordRoute::class) == true

    val loads = koinInject<RemoteLoadMonitor>()
    val loadFailed by loads.hasFailure.collectAsStateWithLifecycle()
    val retryScope = rememberCoroutineScope()
    // Landscape keeps the content's height: the tabs move to a rail on the left, the player slims down.
    BoxWithConstraints {
        val isLandscape = maxWidth > maxHeight
        Scaffold(
            topBar = {
                if (loadFailed) Column(Modifier.padding(Spacing.m)) {
                    Text("Actualisation impossible. Les données peuvent être incomplètes ou anciennes.")
                    TextButton(onClick = { retryScope.launch { loads.retry() } }) { Text("Réessayer") }
                }
            },
            bottomBar = {
                // Floating: the player card (while something plays) stacked over the tab pill, both
                // clear of the screen edges. Pushed screens keep the player only.
                if (!isOnStage) Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(start = Spacing.l, end = Spacing.l, top = if (isLandscape) Spacing.xs else Spacing.s, bottom = if (isLandscape) Spacing.xs else Spacing.m),
                ) {
                    if (!isRecordingVoice) MiniPlayer(compact = isLandscape)
                    if (isOnTab && !isLandscape) TabBar(navController, destination)
                }
            },
        ) { padding ->
            Row(Modifier.padding(padding)) {
                if (isOnTab && isLandscape) {
                    Box(
                        Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start)).padding(start = Spacing.s, top = Spacing.s, bottom = Spacing.s),
                        contentAlignment = Alignment.Center,
                    ) { TabBar(navController, destination, asRail = true) }
                }
                Box(Modifier.weight(1f)) {
                    AppNavHost(navController, onSignOut)
                    // The tabs scroll right up to the status bar: a night veil fades them out there instead of
                    // cutting cards and titles under the clock.
                    if (isOnTab) StatusBarVeil(Modifier.align(Alignment.TopCenter))
                }
            }
        }
    }
}

/** Every screen of the signed-in app, tabs and pushed screens alike. */
@Composable
private fun AppNavHost(navController: NavHostController, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    val openSong: (String) -> Unit = { id -> navController.navigate(SongRoute(id)) }
    NavHost(navController, startDestination = WeekRoute, modifier = modifier) {
        composable<WeekRoute> {
            WeekScreen(
                onSongClick = openSong,
                onEditWeek = { date -> navController.navigate(SetlistEditorRoute(date?.toString())) },
            )
        }
        composable<SetlistEditorRoute> { entry ->
            SetlistEditorScreen(
                serviceDate = entry.toRoute<SetlistEditorRoute>().serviceDate?.let(LocalDate::parse),
                onBack = { navController.popBackStack() },
                onPublished = { navController.popBackStack() },
            )
        }
        composable<LibraryRoute> {
            LibraryScreen(onSongClick = openSong, onAddSong = { navController.navigate(SongEditorRoute()) })
        }
        composable<PianoScaleCoachRoute> {
            com.xperiencemusic.shared.ui.learning.PianoScaleScreen(onBack = { navController.popBackStack() })
        }
        composable<LearningAssessmentRoute> { entry ->
            com.xperiencemusic.shared.ui.learning.AssessmentScreen(entry.toRoute<LearningAssessmentRoute>().moduleId, onBack = { navController.popBackStack() })
        }
        composable<PianoLearningRoute> { entry ->
            val moduleId = entry.toRoute<PianoLearningRoute>().moduleId
            com.xperiencemusic.shared.ui.learning.LearningScreen(
                onBack = { navController.popBackStack() }, moduleId = moduleId,
                onModule = { id -> navController.navigate(PianoLearningRoute(id)) { popUpTo<PianoLearningRoute> { inclusive = true } } },
                onRecord = { navController.navigate(LearningAssessmentRoute(moduleId)) },
                onScaleCoach = { navController.navigate(PianoScaleCoachRoute) },
            )
        }
        composable<ProgressRoute> {
            ProgressScreen(
                onOpenLearning = { moduleId -> navController.navigate(PianoLearningRoute(moduleId)) },
                onExerciseClick = { navController.navigate(ExerciseRoute(it)) },
                onManageMembers = { navController.navigate(MembersRoute) },
                onRecordVoice = { navController.navigate(VoiceRecordRoute(it)) },
                onReview = { navController.navigate(VoiceReviewRoute) },
                onValidate = { navController.navigate(GrowthReviewRoute) },
                onSignOut = onSignOut,
                onProfile = { navController.navigate(ProfileRoute) },
            )
        }
        composable<ProfileRoute> { ProfileScreen(onBack = { navController.popBackStack() }) }
        composable<PlanningRoute> {
            PlanningScreen(
                onMyAnswers = { month -> navController.navigate(AvailabilityRoute(month.toString())) },
                onServiceClick = { date -> navController.navigate(ServiceRoute(date.toString())) },
            )
        }
        composable<AvailabilityRoute> { entry ->
            AvailabilityScreen(
                monthStart = LocalDate.parse(entry.toRoute<AvailabilityRoute>().monthStart),
                onBack = { navController.popBackStack() },
            )
        }
        composable<ServiceRoute> { entry ->
            ServiceDetailScreen(
                date = LocalDate.parse(entry.toRoute<ServiceRoute>().date),
                onBack = { navController.popBackStack() },
            )
        }
        composable<MembersRoute> { MembersScreen(onBack = { navController.popBackStack() }) }
        composable<VoiceRecordRoute> { entry ->
            VoiceRecordScreen(songId = entry.toRoute<VoiceRecordRoute>().songId, onBack = { navController.popBackStack() })
        }
        composable<GrowthReviewRoute> { GrowthReviewScreen(onBack = { navController.popBackStack() }) }
        composable<VoiceReviewRoute> { VoiceReviewScreen(onBack = { navController.popBackStack() }) }
        composable<ExerciseRoute> { entry ->
            ExerciseScreen(exerciseId = entry.toRoute<ExerciseRoute>().exerciseId, onBack = { navController.popBackStack() })
        }
        composable<SongRoute> { entry ->
            val songId = entry.toRoute<SongRoute>().songId
            SongDetailScreen(
                songId = songId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(SongEditorRoute(songId)) },
                onAddRecording = { navController.navigate(RecordingFormRoute(songId)) },
                onOpenStage = { transpose, notation ->
                    navController.navigate(StageRoute(songId, transpose, notation.name))
                },
                onRecordVoice = { navController.navigate(VoiceRecordRoute(songId)) },
            )
        }
        composable<StageRoute> { entry ->
            val route = entry.toRoute<StageRoute>()
            // "Suivant" swaps the stage for another one, so the song detail underneath may be
            // the starting song: closing replaces it with the song that was on screen.
            val startedFrom = navController.previousBackStackEntry
                ?.takeIf { it.destination.hasRoute(SongRoute::class) }
                ?.toRoute<SongRoute>()?.songId
            val closeStage: () -> Unit = {
                if (startedFrom == null || startedFrom == route.songId) {
                    navController.popBackStack()
                } else {
                    navController.navigate(SongRoute(route.songId)) { popUpTo<SongRoute> { inclusive = true } }
                }
            }
            SystemBackHandler(enabled = startedFrom != null && startedFrom != route.songId, onBack = closeStage)
            StageScreen(
                songId = route.songId,
                transposeSemitones = route.transposeSemitones,
                notation = Notation.valueOf(route.notation),
                onClose = closeStage,
                onOpenSong = { nextId ->
                    // Other songs open in their own key, keeping the notation.
                    navController.navigate(StageRoute(nextId, 0, route.notation)) {
                        popUpTo<StageRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<RecordingFormRoute> { entry ->
            RecordingFormScreen(
                songId = entry.toRoute<RecordingFormRoute>().songId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable<SongEditorRoute> { entry ->
            val songId = entry.toRoute<SongEditorRoute>().songId
            SongEditorScreen(
                songId = songId,
                onBack = { navController.popBackStack() },
                onSaved = { savedId ->
                    if (songId == null) {
                        // A new song opens on its page, with the editor removed from the back stack.
                        navController.navigate(SongRoute(savedId)) { popUpTo<SongEditorRoute> { inclusive = true } }
                    } else {
                        navController.popBackStack()
                    }
                },
                // The song page under the editor no longer exists either.
                onDeleted = { navController.popBackStack<SongRoute>(inclusive = true) },
            )
        }
    }
}

@Composable
private fun StatusBarVeil(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(Spacing.l)
            .background(Brush.verticalGradient(listOf(Palette.Night, Palette.Night.copy(alpha = 0f)))),
    )
}

@Composable
private fun TabBar(navController: NavHostController, destination: NavDestination?, asRail: Boolean = false) {
    val items = tabs.map { tab ->
        FloatingTab(
            label = stringResource(tab.shortLabel),
            description = stringResource(tab.label),
            icon = tab.icon,
            isSelected = destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true,
            onClick = {
                navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }
    if (asRail) FloatingTabRail(items) else FloatingTabBar(items)
}
