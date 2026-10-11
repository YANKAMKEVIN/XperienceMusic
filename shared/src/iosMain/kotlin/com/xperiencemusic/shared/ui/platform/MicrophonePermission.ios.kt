package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberMicrophonePermission(): MicrophonePermission {
    var isGranted by remember {
        mutableStateOf(AVAudioSession.sharedInstance().recordPermission == AVAudioSessionRecordPermissionGranted)
    }
    return MicrophonePermission(isGranted) { onResult ->
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            // The answer arrives on a background queue.
            dispatch_async(dispatch_get_main_queue()) {
                isGranted = granted
                onResult(granted)
            }
        }
    }
}
