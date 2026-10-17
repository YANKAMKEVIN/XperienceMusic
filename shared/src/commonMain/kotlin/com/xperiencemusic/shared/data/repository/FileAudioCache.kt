package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.remote.AUDIO_BUCKET
import com.xperiencemusic.shared.domain.repository.AudioCache
import com.xperiencemusic.shared.domain.repository.FileStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Downloads from the private audio bucket into the app's storage, one file per audio path. */
class FileAudioCache(
    private val client: SupabaseClient,
    private val files: FileStore,
) : AudioCache {
    private var generation = 0
    private val _cachedPaths = MutableStateFlow(files.list(DIRECTORY).map(::audioPathOf).toSet())
    override val cachedPaths: StateFlow<Set<String>> = _cachedPaths.asStateFlow()

    override fun localUrl(audioPath: String): String? = files.fileUrl(fileOf(audioPath))

    override suspend fun download(audioPath: String) {
        val started = generation
        val bytes = client.storage.from(AUDIO_BUCKET).downloadAuthenticated(audioPath)
        if (started != generation) throw kotlin.coroutines.cancellation.CancellationException("Session ended")
        files.write(fileOf(audioPath), bytes)
        _cachedPaths.update { it + audioPath }
    }

    override suspend fun clear() {
        generation++
        files.delete(DIRECTORY)
        _cachedPaths.value = emptySet()
    }

    private companion object {
        const val DIRECTORY = "audio"

        // Bucket paths contain "/"; flatten them into one file name, reversibly.
        fun fileOf(audioPath: String) = "$DIRECTORY/" + audioPath.replace("/", "__")

        fun audioPathOf(fileName: String) = fileName.replace("__", "/")
    }
}
