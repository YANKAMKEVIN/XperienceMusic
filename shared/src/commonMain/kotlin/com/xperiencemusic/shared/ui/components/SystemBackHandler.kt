package com.xperiencemusic.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

/**
 * Catches the system back (Android gesture or button, iOS edge swipe) while [enabled].
 *
 * CMP 1.11 deprecates this `BackHandler` for `NavigationEventHandler`, but that one needs
 * `navigationevent-compose`, whose dispatcher the iOS `ComposeUIViewController` does not provide
 * yet: this one works on both. Kept in one place to move over later.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
