package com.xperiencemusic.shared.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.tab_library
import com.xperiencemusic.shared.resources.tab_progress
import com.xperiencemusic.shared.resources.tab_week
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.exercise.ExerciseScreen
import com.xperiencemusic.shared.ui.library.LibraryScreen
import com.xperiencemusic.shared.ui.members.MembersScreen
import com.xperiencemusic.shared.ui.player.MiniPlayer
import com.xperiencemusic.shared.ui.progress.ProgressScreen
import com.xperiencemusic.shared.ui.recordingform.RecordingFormScreen
import com.xperiencemusic.shared.ui.setlisteditor.SetlistEditorScreen
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.ui.song.SongDetailScreen
import com.xperiencemusic.shared.ui.stage.StageScreen
import com.xperiencemusic.shared.ui.songeditor.SongEditorScreen
import com.xperiencemusic.shared.ui.week.WeekScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.LocalDate
import kotlin.reflect.KClass

private data class Tab(val route: Any, val routeClass: KClass<*>, val label: StringResource, val icon: ImageVector)

private val tabs = listOf(
    Tab(WeekRoute, WeekRoute::class, Res.string.tab_week, XmIcons.Notes),
    Tab(LibraryRoute, LibraryRoute::class, Res.string.tab_library, XmIcons.Book),
    Tab(ProgressRoute, ProgressRoute::class, Res.string.tab_progress, XmIcons.Bars),
)

/** The signed-in shell: three tabs, with the song detail pushed on top of them. */
@Composable
fun MainScaffold(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isOnTab = tabs.any { tab -> destination?.hasRoute(tab.routeClass) == true }
    // The stage takes the whole screen: no tabs, no mini player.
    val isOnStage = destination?.hasRoute(StageRoute::class) == true

    Scaffold(
        bottomBar = {
            if (!isOnStage) Column {
                // Above the tabs, and alone at the bottom of pushed screens, so playback stays reachable.
                MiniPlayer(isAtScreenBottom = !isOnTab)
                if (isOnTab) TabBar(navController, destination)
            }
        },
    ) { padding ->
        val openSong: (String) -> Unit = { id -> navController.navigate(SongRoute(id)) }
        NavHost(navController, startDestination = WeekRoute, modifier = Modifier.padding(padding)) {
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
            composable<ProgressRoute> {
                ProgressScreen(
                    onExerciseClick = { navController.navigate(ExerciseRoute(it)) },
                    onManageMembers = { navController.navigate(MembersRoute) },
                    onSignOut = onSignOut,
                )
            }
            composable<MembersRoute> { MembersScreen(onBack = { navController.popBackStack() }) }
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
                )
            }
            composable<StageRoute> { entry ->
                val route = entry.toRoute<StageRoute>()
                StageScreen(
                    songId = route.songId,
                    transposeSemitones = route.transposeSemitones,
                    notation = Notation.valueOf(route.notation),
                    onClose = { navController.popBackStack() },
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
}

@Composable
private fun TabBar(navController: NavHostController, destination: NavDestination?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        tabs.forEach { tab ->
            val label = stringResource(tab.label)
            NavigationBarItem(
                selected = destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}
