package com.xperiencemusic.shared.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.ui.account.AccountScreen
import com.xperiencemusic.shared.ui.components.SystemBackHandler
import com.xperiencemusic.shared.ui.exercise.ExerciseScreen
import com.xperiencemusic.shared.ui.learning.AssessmentScreen
import com.xperiencemusic.shared.ui.learning.LearningScreen
import com.xperiencemusic.shared.ui.learning.PianoScaleScreen
import com.xperiencemusic.shared.ui.library.LibraryScreen
import com.xperiencemusic.shared.ui.members.MembersScreen
import com.xperiencemusic.shared.ui.planning.AvailabilityScreen
import com.xperiencemusic.shared.ui.planning.PlanningScreen
import com.xperiencemusic.shared.ui.planning.ServiceDetailScreen
import com.xperiencemusic.shared.ui.profile.ProfileScreen
import com.xperiencemusic.shared.ui.progress.GrowthReviewScreen
import com.xperiencemusic.shared.ui.progress.ProgressScreen
import com.xperiencemusic.shared.ui.recordingform.RecordingFormScreen
import com.xperiencemusic.shared.ui.rehearsal.RehearsalUploadScreen
import com.xperiencemusic.shared.ui.setlisteditor.SetlistEditorScreen
import com.xperiencemusic.shared.ui.song.SongDetailScreen
import com.xperiencemusic.shared.ui.songeditor.SongEditorScreen
import com.xperiencemusic.shared.ui.stage.StageScreen
import com.xperiencemusic.shared.ui.voice.VoiceRecordScreen
import com.xperiencemusic.shared.ui.voice.VoiceReviewScreen
import com.xperiencemusic.shared.ui.week.WeekScreen
import kotlinx.datetime.LocalDate

/**
 * Every screen of the signed-in app, tabs and pushed screens alike. The movement says where one is
 * going: tabs cross-fade (another section, same level), a pushed screen slides in from the side
 * and back out (down into a detail), and the stage grows out of the song page.
 */
@Composable
internal fun AppNavHost(navController: NavHostController, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    NavHost(
        navController,
        startDestination = WeekRoute,
        modifier = modifier,
        enterTransition = {
            when {
                isTabSwitch() -> fadeIn(tween(TAB_FADE_MS))
                targetState.isStage() -> scaleIn(tween(STAGE_MS), initialScale = STAGE_SCALE) + fadeIn(tween(STAGE_MS))
                // The stage closed on another song than the one it opened from: that page takes its place.
                initialState.isStage() -> fadeIn(tween(STAGE_MS))
                else -> slideIntoContainer(SlideDirection.Start, tween(PUSH_MS)) + fadeIn(tween(PUSH_MS))
            }
        },
        exitTransition = {
            when {
                isTabSwitch() -> fadeOut(tween(TAB_FADE_MS))
                targetState.isStage() -> fadeOut(tween(STAGE_MS))
                initialState.isStage() -> scaleOut(tween(STAGE_MS), targetScale = STAGE_SCALE) + fadeOut(tween(STAGE_MS))
                // The screen underneath only steps aside a little, and dims.
                else -> slideOutOfContainer(SlideDirection.Start, tween(PUSH_MS)) { it / UNDERNEATH_SHARE } + fadeOut(tween(PUSH_MS))
            }
        },
        popEnterTransition = {
            when {
                isTabSwitch() -> fadeIn(tween(TAB_FADE_MS))
                initialState.isStage() -> fadeIn(tween(STAGE_MS))
                else -> slideIntoContainer(SlideDirection.End, tween(PUSH_MS)) { it / UNDERNEATH_SHARE } + fadeIn(tween(PUSH_MS))
            }
        },
        popExitTransition = {
            when {
                isTabSwitch() -> fadeOut(tween(TAB_FADE_MS))
                initialState.isStage() -> scaleOut(tween(STAGE_MS), targetScale = STAGE_SCALE) + fadeOut(tween(STAGE_MS))
                else -> slideOutOfContainer(SlideDirection.End, tween(PUSH_MS)) + fadeOut(tween(PUSH_MS))
            }
        },
    ) {
        weekGraph(navController)
        songsGraph(navController)
        planningGraph(navController)
        progressGraph(navController)
        learningGraph(navController)
        accountGraph(navController, onSignOut)
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch() =
    isTab(initialState.destination) && isTab(targetState.destination)

private fun NavBackStackEntry.isStage() = destination.hasRoute(StageRoute::class)

private const val TAB_FADE_MS = 220
private const val PUSH_MS = 300
private const val STAGE_MS = 260
/** The stage starts from nine tenths of the screen: enough to read as "growing", not as a jump. */
private const val STAGE_SCALE = 0.9f
/** How little the screen underneath moves while another slides over it: a quarter of the width. */
private const val UNDERNEATH_SHARE = 4

private fun NavHostController.openSong(id: String) = navigate(SongRoute(id))

/** The Week tab and its setlist editor. */
private fun NavGraphBuilder.weekGraph(navController: NavHostController) {
    composable<WeekRoute> {
        WeekScreen(
            onSongClick = navController::openSong,
            onEditWeek = { date -> navController.navigate(SetlistEditorRoute(date?.toString())) },
            onUploadRehearsal = { date -> navController.navigate(RehearsalUploadRoute(date.toString())) },
            onAccount = { navController.navigate(AccountRoute) },
        )
    }
    composable<SetlistEditorRoute> { entry ->
        SetlistEditorScreen(
            serviceDate = entry.toRoute<SetlistEditorRoute>().serviceDate?.let(LocalDate::parse),
            onBack = { navController.popBackStack() },
            onPublished = { navController.popBackStack() },
        )
    }
}

/** The library: songs, their recordings, the editor and the stage view. */
private fun NavGraphBuilder.songsGraph(navController: NavHostController) {
    composable<LibraryRoute> {
        LibraryScreen(onSongClick = navController::openSong, onAddSong = { navController.navigate(SongEditorRoute()) })
    }
    composable<SongRoute> { entry ->
        val songId = entry.toRoute<SongRoute>().songId
        SongDetailScreen(
            songId = songId,
            onBack = { navController.popBackStack() },
            onEdit = { navController.navigate(SongEditorRoute(songId)) },
            onAddRecording = { navController.navigate(RecordingFormRoute(songId)) },
            onOpenStage = { transpose, notation, lyricsOnly ->
                navController.navigate(StageRoute(songId, transpose, notation.name, lyricsOnly))
            },
            onRecordVoice = { navController.navigate(VoiceRecordRoute(songId)) },
        )
    }
    stageGraph(navController)
    composable<RehearsalUploadRoute> { entry ->
        RehearsalUploadScreen(
            serviceDate = LocalDate.parse(entry.toRoute<RehearsalUploadRoute>().serviceDate),
            onBack = { navController.popBackStack() },
            onAddTake = { songId, date -> navController.navigate(RecordingFormRoute(songId, date.toString())) },
        )
    }
    composable<RecordingFormRoute> { entry ->
        RecordingFormScreen(
            songId = entry.toRoute<RecordingFormRoute>().songId,
            rehearsalDate = entry.toRoute<RecordingFormRoute>().rehearsalDate?.let(LocalDate::parse),
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

/** The stage: the chart in full screen, moving on to the week's other songs. */
private fun NavGraphBuilder.stageGraph(navController: NavHostController) {
    // Which way the setlist was last walked: the next song comes in from the right, the previous from the left.
    var isForward = true
    val slide = tween<IntOffset>(STAGE_SLIDE_MS)
    fun AnimatedContentTransitionScope<NavBackStackEntry>.isBetweenSongs() =
        initialState.destination.hasRoute(StageRoute::class) && targetState.destination.hasRoute(StageRoute::class)
    composable<StageRoute>(
        // Only from one song of the setlist to another; opening and closing the stage keep the host's transition.
        enterTransition = { if (isBetweenSongs()) slideInHorizontally(slide) { if (isForward) it else -it } else null },
        exitTransition = { if (isBetweenSongs()) slideOutHorizontally(slide) { if (isForward) -it else it } else null },
    ) { entry ->
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
            lyricsOnly = route.lyricsOnly,
            onClose = closeStage,
            onOpenSong = { nextId, forward ->
                isForward = forward
                // Other songs open in their own key, keeping the notation and the chords on or off.
                navController.navigate(StageRoute(nextId, 0, route.notation, route.lyricsOnly)) {
                    popUpTo<StageRoute> { inclusive = true }
                }
            },
        )
    }
}

private const val STAGE_SLIDE_MS = 280

/** The Planning tab: the month's services and the member's answers. */
private fun NavGraphBuilder.planningGraph(navController: NavHostController) {
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
}

/** The Progress tab: exercises, sung parts and what the reviewer answers. */
private fun NavGraphBuilder.progressGraph(navController: NavHostController) {
    composable<ProgressRoute> {
        ProgressScreen(
            onOpenLearning = { moduleId -> navController.navigate(PianoLearningRoute(moduleId)) },
            onExerciseClick = { navController.navigate(ExerciseRoute(it)) },
            onRecordVoice = { navController.navigate(VoiceRecordRoute(it)) },
            onReview = { navController.navigate(VoiceReviewRoute) },
            onValidate = { navController.navigate(GrowthReviewRoute) },
        )
    }
    composable<ExerciseRoute> { entry ->
        ExerciseScreen(exerciseId = entry.toRoute<ExerciseRoute>().exerciseId, onBack = { navController.popBackStack() })
    }
    composable<VoiceRecordRoute> { entry ->
        VoiceRecordScreen(songId = entry.toRoute<VoiceRecordRoute>().songId, onBack = { navController.popBackStack() })
    }
    composable<VoiceReviewRoute> { VoiceReviewScreen(onBack = { navController.popBackStack() }) }
    composable<GrowthReviewRoute> { GrowthReviewScreen(onBack = { navController.popBackStack() }) }
}

/** The guided learning path, its recorded objectives and the scale workshop. */
private fun NavGraphBuilder.learningGraph(navController: NavHostController) {
    composable<PianoLearningRoute> { entry ->
        val moduleId = entry.toRoute<PianoLearningRoute>().moduleId
        LearningScreen(
            onBack = { navController.popBackStack() },
            moduleId = moduleId,
            onModule = { id -> navController.navigate(PianoLearningRoute(id)) { popUpTo<PianoLearningRoute> { inclusive = true } } },
            onRecord = { navController.navigate(LearningAssessmentRoute(moduleId)) },
            onScaleCoach = { navController.navigate(PianoScaleCoachRoute) },
        )
    }
    composable<LearningAssessmentRoute> { entry ->
        AssessmentScreen(entry.toRoute<LearningAssessmentRoute>().moduleId, onBack = { navController.popBackStack() })
    }
    composable<PianoScaleCoachRoute> { PianoScaleScreen(onBack = { navController.popBackStack() }) }
}

/** The member's account, profile and, for admins, the band's members. */
private fun NavGraphBuilder.accountGraph(navController: NavHostController, onSignOut: () -> Unit) {
    composable<AccountRoute> {
        AccountScreen(
            onBack = { navController.popBackStack() },
            onEditProfile = { navController.navigate(ProfileRoute) },
            onReview = { navController.navigate(VoiceReviewRoute) },
            onValidate = { navController.navigate(GrowthReviewRoute) },
            onManageMembers = { navController.navigate(MembersRoute) },
            onSignOut = onSignOut,
        )
    }
    composable<ProfileRoute> { ProfileScreen(onBack = { navController.popBackStack() }) }
    composable<MembersRoute> { MembersScreen(onBack = { navController.popBackStack() }) }
}
