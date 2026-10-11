package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable

/** Microphone permission for the current screen; [request] shows the system prompt. */
class MicrophonePermission(
    val isGranted: Boolean,
    val request: (onResult: (Boolean) -> Unit) -> Unit,
)

@Composable
expect fun rememberMicrophonePermission(): MicrophonePermission
