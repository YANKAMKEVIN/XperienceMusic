package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import com.xperiencemusic.shared.domain.model.AudioCompression
import com.xperiencemusic.shared.domain.model.AudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVURLAsset
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeAudio
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun rememberAudioFilePicker(onCompressing: (Float) -> Unit, onResult: (AudioFile?) -> Unit): () -> Unit {
    val currentOnResult = rememberUpdatedState(onResult)
    val currentOnCompressing = rememberUpdatedState(onCompressing)
    val scope = rememberCoroutineScope()
    // UIKit only keeps a weak reference to the delegate; remember keeps it alive.
    val delegate = remember {
        AudioPickerDelegate { url ->
            if (url == null) {
                currentOnResult.value(null)
            } else {
                scope.launch {
                    // Reading and re-encoding a long rehearsal must not freeze the screen.
                    val file = withContext(Dispatchers.Default) { readAudio(url) { currentOnCompressing.value(it) } }
                    currentOnResult.value(file)
                }
            }
        }
    }
    return remember(delegate) {
        {
            // asCopy: the file is copied into the app sandbox, no security-scoped access needed.
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeAudio), asCopy = true)
            picker.delegate = delegate
            picker.allowsMultipleSelection = false
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class AudioPickerDelegate(private val onResult: (NSURL?) -> Unit) :
    NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onResult(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readAudio(url: NSURL, onCompressing: (Float) -> Unit): AudioFile? {
    val name = url.lastPathComponent ?: "audio"
    val seconds = CMTimeGetSeconds(AVURLAsset.URLAssetWithURL(url, options = null).duration)
    val duration = seconds.takeIf { it.isFinite() && it > 0 }?.toInt()
    val size = url.path?.let { path ->
        (NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)?.get(NSFileSize) as? NSNumber)?.longLongValue
    } ?: -1L
    AudioCompression.targetBitrate(size, duration)?.let { bitrate ->
        compress(url, name, size, duration, bitrate, onCompressing)?.let { return it }
    }
    val bytes = readBytes(url) ?: return null
    return AudioFile(
        fileName = name,
        mimeType = UTType.typeWithFilenameExtension(url.pathExtension.orEmpty())?.preferredMIMEType ?: "audio/mpeg",
        bytes = bytes,
        durationSeconds = duration,
    )
}

/** Null when the file cannot be re-encoded: it then goes as it is (or is refused if too big). */
@OptIn(ExperimentalForeignApi::class)
private fun compress(url: NSURL, name: String, size: Long, duration: Int?, bitrate: Int, onCompressing: (Float) -> Unit): AudioFile? {
    val out = NSURL.fileURLWithPath(NSTemporaryDirectory() + "upload-" + NSUUID().UUIDString + ".m4a")
    try {
        onCompressing(0f)
        if (!transcodeToAac(url, out, bitrate, onCompressing)) return null
        val bytes = readBytes(out)?.takeIf { it.isNotEmpty() && it.size < size } ?: return null
        return AudioFile(
            fileName = name.substringBeforeLast('.', name) + ".m4a",
            mimeType = "audio/mp4",
            bytes = bytes,
            durationSeconds = duration,
            compressedFromBytes = size,
        )
    } finally {
        NSFileManager.defaultManager.removeItemAtURL(out, error = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readBytes(url: NSURL): ByteArray? {
    val data = NSData.dataWithContentsOfURL(url) ?: return null
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
    }
    return bytes
}

private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) controller = controller.presentedViewController
    return controller
}
