package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable

/**
 * Microphone permission for the current screen; [request] shows the system prompt. Once refused,
 * Android and iOS stop asking: [openSettings] opens the app's page in the phone settings.
 */
class MicrophonePermission(
    val isGranted: Boolean,
    val request: (onResult: (Boolean) -> Unit) -> Unit,
    val openSettings: () -> Unit = {},
)

@Composable
expect fun rememberMicrophonePermission(): MicrophonePermission
