package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.domain.repository.DevicePreferences
import com.xperiencemusic.shared.domain.repository.FileStore

/** One "key=value" line per choice, in a file outside the member's data. */
class FileDevicePreferences(private val files: FileStore) : DevicePreferences {
    private val values: MutableMap<String, String> by lazy {
        files.readText(PATH).orEmpty().lines()
            .mapNotNull { line -> line.split(SEPARATOR, limit = 2).takeIf { it.size == 2 } }
            .associateTo(mutableMapOf()) { (key, value) -> key to value }
    }

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String) {
        if (values[key] == value) return
        values[key] = value
        // A full disk must not break the screen that changed the setting: it is only not remembered.
        runCatching { files.writeText(PATH, values.entries.joinToString("\n") { "${it.key}$SEPARATOR${it.value}" }) }
    }

    private companion object {
        const val PATH = "device/preferences.txt"
        const val SEPARATOR = "="
    }
}
