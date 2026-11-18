package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable
import com.xperiencemusic.shared.domain.model.AudioFile

/**
 * Returns a function that opens the system file picker restricted to audio files. A large file is
 * first re-encoded to mono AAC (see `AudioCompression`), [onCompressing] then reporting its progress
 * from 0 to 1. [onResult] receives the file read into memory, or null when the user cancels or the
 * file is unreadable. A file that cannot be re-encoded is handed over as it is.
 */
@Composable
expect fun rememberAudioFilePicker(onCompressing: (Float) -> Unit, onResult: (AudioFile?) -> Unit): () -> Unit
