package com.xperiencemusic.shared.platform

import android.content.Context
import android.net.Uri
import com.xperiencemusic.shared.domain.repository.FileStore
import java.io.File

/** Files under the app's internal storage, which only the app can read. */
class AndroidFileStore(context: Context) : FileStore {
    private val root = context.filesDir

    override fun write(relativePath: String, bytes: ByteArray): String {
        val file = resolve(relativePath).apply { parentFile?.mkdirs() }
        // Write then rename, so a crash never leaves a half-written file behind.
        val partial = File(file.parentFile, file.name + ".part")
        partial.writeBytes(bytes)
        partial.renameTo(file)
        return Uri.fromFile(file).toString()
    }

    override fun fileUrl(relativePath: String): String? =
        resolve(relativePath).takeIf { it.isFile }?.let { Uri.fromFile(it).toString() }

    override fun readText(relativePath: String): String? =
        resolve(relativePath).takeIf { it.isFile }?.readText()

    override fun writeText(relativePath: String, text: String) {
        write(relativePath, text.encodeToByteArray())
    }

    override fun list(directory: String): List<String> =
        resolve(directory).listFiles()?.filter { it.isFile && !it.name.endsWith(".part") }?.map { it.name }.orEmpty()

    override fun delete(relativePath: String) {
        resolve(relativePath).deleteRecursively()
    }

    private fun resolve(relativePath: String) = File(root, relativePath)
}
