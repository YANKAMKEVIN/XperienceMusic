package com.xperiencemusic.shared.ui.platform

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberMicrophonePermission(): MicrophonePermission {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var pendingCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isGranted = granted
        pendingCallback?.invoke(granted)
        pendingCallback = null
    }
    return MicrophonePermission(isGranted) { onResult ->
        if (isGranted) {
            onResult(true)
        } else {
            pendingCallback = onResult
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
