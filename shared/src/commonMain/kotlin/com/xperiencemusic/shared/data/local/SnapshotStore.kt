package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.domain.repository.FileStore
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Last copy of server data, so the app opens and shows the band's songs without network. Stored as
 * the network DTOs: the snapshot is exactly what the server last said.
 */
class SnapshotStore(private val files: FileStore) {
    private val json = Json { ignoreUnknownKeys = true }

    fun <T> load(name: String, serializer: KSerializer<T>): T? =
        files.readText(path(name))?.let { text -> runCatching { json.decodeFromString(serializer, text) }.getOrNull() }

    fun <T> save(name: String, serializer: KSerializer<T>, value: T) {
        files.writeText(path(name), json.encodeToString(serializer, value))
    }

    fun clear() = files.delete(DIRECTORY)

    private fun path(name: String) = "$DIRECTORY/$name.json"

    private companion object {
        const val DIRECTORY = "snapshots"
    }
}
