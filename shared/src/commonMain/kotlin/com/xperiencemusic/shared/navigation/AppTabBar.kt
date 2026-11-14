package com.xperiencemusic.shared.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.tab_library
import com.xperiencemusic.shared.resources.tab_planning
import com.xperiencemusic.shared.resources.tab_progress
import com.xperiencemusic.shared.resources.tab_progress_short
import com.xperiencemusic.shared.resources.tab_week
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.reflect.KClass

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

/** Whether [destination] is one of the four tabs (rather than a screen pushed over them). */
internal fun isTab(destination: NavDestination?): Boolean = tabs.any { tab -> destination?.hasRoute(tab.routeClass) == true }

/** The four tabs, as the floating bar at the bottom or, with [asRail], the rail on the left. */
@Composable
internal fun TabBar(navController: NavHostController, destination: NavDestination?, asRail: Boolean = false) {
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

/**
 * The tabs scroll right up to the status bar: this veil fades them out there instead of cutting
 * cards and titles under the clock.
 */
@Composable
internal fun StatusBarVeil(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier
            .fillMaxWidth()
            .height(Spacing.l)
            .background(Brush.verticalGradient(listOf(background, background.copy(alpha = 0f)))),
    )
}
