package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable

/** Hides the status and navigation bars while this composable is shown (stage mode). */
@Composable
expect fun HideSystemBars()
