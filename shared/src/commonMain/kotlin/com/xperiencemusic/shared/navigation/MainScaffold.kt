package com.xperiencemusic.shared.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.remote_load_failed
import com.xperiencemusic.shared.resources.remote_load_retry
import com.xperiencemusic.shared.ui.platform.AskNotificationPermission
import com.xperiencemusic.shared.ui.player.MiniPlayer
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * The signed-in shell: four tabs, with the song detail pushed on top of them. [hasLoadFailure]
 * shows a banner above every screen, whose button calls [onRetryLoads].
 */
@Composable
fun MainScaffold(onSignOut: () -> Unit, hasLoadFailure: Boolean, onRetryLoads: () -> Unit) {
    val navController = rememberNavController()
    AskNotificationPermission()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isOnTab = isTab(destination)
    // The stage takes the whole screen: no tabs, no mini player.
    val isOnStage = destination?.hasRoute(StageRoute::class) == true
    // Recording a voice: the guide plays in the earphones, a player card would only be in the way.
    val isRecordingVoice = destination?.hasRoute(VoiceRecordRoute::class) == true

    // Landscape keeps the content's height: the tabs move to a rail on the left, the player slims down.
    BoxWithConstraints {
        val isLandscape = maxWidth > maxHeight
        Scaffold(
            topBar = {
                if (hasLoadFailure) RemoteLoadBanner(onRetry = onRetryLoads)
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
                    if (isOnTab) StatusBarVeil(Modifier.align(Alignment.TopCenter))
                }
            }
        }
    }
}

/** Some data could not be fetched: the screens may be incomplete or out of date. */
@Composable
private fun RemoteLoadBanner(onRetry: () -> Unit) {
    Column(Modifier.padding(Spacing.m)) {
        Text(stringResource(Res.string.remote_load_failed))
        TextButton(onClick = onRetry) { Text(stringResource(Res.string.remote_load_retry)) }
    }
}
