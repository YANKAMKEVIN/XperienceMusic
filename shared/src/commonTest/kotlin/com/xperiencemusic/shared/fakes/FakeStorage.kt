package com.xperiencemusic.shared.fakes

import com.xperiencemusic.shared.domain.repository.AudioCache
import com.xperiencemusic.shared.domain.repository.FileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeFileStore : FileStore {
    val files = mutableMapOf<String, ByteArray>()

    override fun write(relativePath: String, bytes: ByteArray): String {
        files[relativePath] = bytes
        return "file:///app/$relativePath"
    }

    override fun fileUrl(relativePath: String): String? = if (relativePath in files) "file:///app/$relativePath" else null

    override fun readText(relativePath: String): String? = files[relativePath]?.decodeToString()

    override fun writeText(relativePath: String, text: String) {
        write(relativePath, text.encodeToByteArray())
    }

    override fun list(directory: String): List<String> =
        files.keys.filter { it.startsWith("$directory/") }.map { it.removePrefix("$directory/") }

    override fun delete(relativePath: String) {
        files.keys.removeAll { it == relativePath || it.startsWith("$relativePath/") }
    }
}

/** Downloads succeed unless the path is in [failing]. */
class FakeAudioCache(initial: Set<String> = emptySet()) : AudioCache {
    override val cachedPaths = MutableStateFlow(initial)
    val failing = mutableSetOf<String>()
    val downloaded = mutableListOf<String>()

    override fun localUrl(audioPath: String): String? = if (audioPath in cachedPaths.value) "file:///$audioPath" else null

    override suspend fun download(audioPath: String) {
        if (audioPath in failing) error("network down")
        downloaded += audioPath
        cachedPaths.update { it + audioPath }
    }

    override suspend fun clear() {
        cachedPaths.value = emptySet()
    }
}
