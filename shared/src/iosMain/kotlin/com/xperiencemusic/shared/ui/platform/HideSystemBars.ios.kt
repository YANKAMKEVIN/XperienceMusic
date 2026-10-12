package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * SwiftUI owns the status bar and home indicator: ContentView listens through
 * [observeSystemBarsHidden] and hides them while stage mode is on.
 */
private var listener: ((Boolean) -> Unit)? = null

fun observeSystemBarsHidden(onChange: (Boolean) -> Unit) {
    listener = onChange
}

@Composable
actual fun HideSystemBars() {
    DisposableEffect(Unit) {
        listener?.invoke(true)
        onDispose { listener?.invoke(false) }
    }
}
