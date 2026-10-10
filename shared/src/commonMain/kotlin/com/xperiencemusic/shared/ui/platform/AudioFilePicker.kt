package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable
import com.xperiencemusic.shared.domain.model.AudioFile

/**
 * Returns a function that opens the system file picker restricted to audio files. [onResult]
 * receives the file read into memory, or null when the user cancels or the file is unreadable.
 */
@Composable
expect fun rememberAudioFilePicker(onResult: (AudioFile?) -> Unit): () -> Unit
