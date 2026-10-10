package com.xperiencemusic.shared.ui.platform

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.xperiencemusic.shared.domain.model.AudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberAudioFilePicker(onResult: (AudioFile?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            currentOnResult.value(null)
        } else {
            scope.launch {
                val file = withContext(Dispatchers.IO) { runCatching { readAudio(context, uri) }.getOrNull() }
                currentOnResult.value(file)
            }
        }
    }
    return { launcher.launch("audio/*") }
}

private fun readAudio(context: Context, uri: Uri): AudioFile? {
    val resolver = context.contentResolver
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: "audio"
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return AudioFile(
        fileName = name,
        mimeType = resolver.getType(uri) ?: "audio/mpeg",
        bytes = bytes,
        durationSeconds = readDurationSeconds(context, uri),
    )
}

private fun readDurationSeconds(context: Context, uri: Uri): Int? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { (it / 1000).toInt() }
    } catch (e: RuntimeException) {
        null
    } finally {
        retriever.release()
    }
}
