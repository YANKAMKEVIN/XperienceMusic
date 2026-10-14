package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable

/** Asks once to show the planning reminders, where the system wants the app to ask. */
@Composable
expect fun AskNotificationPermission()
