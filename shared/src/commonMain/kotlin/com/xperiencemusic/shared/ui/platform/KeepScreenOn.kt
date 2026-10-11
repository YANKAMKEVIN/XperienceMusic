package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable

/** Keeps the display awake while this composable is shown (stage mode on a music stand). */
@Composable
expect fun KeepScreenOn()
