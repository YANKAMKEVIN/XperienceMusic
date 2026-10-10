package com.xperiencemusic.shared.platform

import com.xperiencemusic.shared.domain.repository.FileStore
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

/** Files under Application Support, private to the app and kept until it deletes them. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosFileStore : FileStore {
    private val manager = NSFileManager.defaultManager
    private val root: String =
        (NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String) +
            "/XperienceMusic"

    override fun write(relativePath: String, bytes: ByteArray): String {
        val path = resolve(relativePath)
        manager.createDirectoryAtPath(path.substringBeforeLast('/'), withIntermediateDirectories = true, attributes = null, error = null)
        val data = if (bytes.isEmpty()) {
            NSData()
        } else {
            bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        }
        // atomically: written to a temp file then moved, never half-written.
        data.writeToFile(path, atomically = true)
        return NSURL.fileURLWithPath(path).absoluteString ?: "file://$path"
    }

    override fun fileUrl(relativePath: String): String? {
        val path = resolve(relativePath)
        return if (manager.fileExistsAtPath(path)) NSURL.fileURLWithPath(path).absoluteString else null
    }

    override fun readText(relativePath: String): String? {
        val path = resolve(relativePath)
        if (!manager.fileExistsAtPath(path)) return null
        return NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null)
    }

    override fun writeText(relativePath: String, text: String) {
        write(relativePath, text.encodeToByteArray())
    }

    override fun list(directory: String): List<String> =
        manager.contentsOfDirectoryAtPath(resolve(directory), error = null)?.map { it as String }.orEmpty()

    override fun delete(relativePath: String) {
        manager.removeItemAtPath(resolve(relativePath), error = null)
    }

    private fun resolve(relativePath: String) = "$root/$relativePath"
}
