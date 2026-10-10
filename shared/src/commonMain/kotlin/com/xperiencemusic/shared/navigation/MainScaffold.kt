package com.xperiencemusic.shared.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.tab_library
import com.xperiencemusic.shared.resources.tab_progress
import com.xperiencemusic.shared.resources.tab_week
import com.xperiencemusic.shared.ui.library.LibraryScreen
import com.xperiencemusic.shared.ui.progress.ProgressScreen
import com.xperiencemusic.shared.ui.song.SongDetailScreen
import com.xperiencemusic.shared.ui.songeditor.SongEditorScreen
import com.xperiencemusic.shared.ui.week.WeekScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.reflect.KClass

private data class Tab(val route: Any, val routeClass: KClass<*>, val label: StringResource, val icon: ImageVector)

private val tabs = listOf(
    Tab(WeekRoute, WeekRoute::class, Res.string.tab_week, Icons.Filled.DateRange),
    Tab(LibraryRoute, LibraryRoute::class, Res.string.tab_library, Icons.AutoMirrored.Filled.List),
    Tab(ProgressRoute, ProgressRoute::class, Res.string.tab_progress, Icons.Filled.CheckCircle),
)

/** The signed-in shell: three tabs, with the song detail pushed on top of them. */
@Composable
fun MainScaffold(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isOnTab = tabs.any { tab -> destination?.hasRoute(tab.routeClass) == true }

    Scaffold(
        bottomBar = {
            if (isOnTab) {
                NavigationBar {
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
                        )
                    }
                }
            }
        },
    ) { padding ->
        val openSong: (String) -> Unit = { id -> navController.navigate(SongRoute(id)) }
        NavHost(navController, startDestination = WeekRoute, modifier = Modifier.padding(padding)) {
            composable<WeekRoute> { WeekScreen(onSongClick = openSong) }
            composable<LibraryRoute> {
                LibraryScreen(onSongClick = openSong, onAddSong = { navController.navigate(SongEditorRoute()) })
            }
            composable<ProgressRoute> { ProgressScreen(onSignOut = onSignOut) }
            composable<SongRoute> { entry ->
                val songId = entry.toRoute<SongRoute>().songId
                SongDetailScreen(
                    songId = songId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(SongEditorRoute(songId)) },
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
                )
            }
        }
    }
}
