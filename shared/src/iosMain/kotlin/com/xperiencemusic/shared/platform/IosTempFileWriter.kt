package com.xperiencemusic.shared.platform

import com.xperiencemusic.shared.domain.repository.TempFileWriter
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile

class IosTempFileWriter : TempFileWriter {
    @OptIn(ExperimentalForeignApi::class)
    override fun write(fileName: String, bytes: ByteArray): String {
        val path = NSTemporaryDirectory() + fileName
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        data.writeToFile(path, atomically = true)
        return NSURL.fileURLWithPath(path).absoluteString ?: "file://$path"
    }
}
