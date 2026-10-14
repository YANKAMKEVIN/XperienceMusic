package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable

/** iOS asks when the first reminders are scheduled (see `IosReminderScheduler`). */
@Composable
actual fun AskNotificationPermission() = Unit
