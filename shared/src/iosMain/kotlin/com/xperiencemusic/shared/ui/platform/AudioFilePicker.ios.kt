package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.xperiencemusic.shared.domain.model.AudioFile
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
actual fun rememberAudioFilePicker(onResult: (AudioFile?) -> Unit): () -> Unit {
    val currentOnResult = rememberUpdatedState(onResult)
    // UIKit only keeps a weak reference to the delegate; remember keeps it alive.
    val delegate = remember { AudioPickerDelegate { currentOnResult.value(it) } }
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

private class AudioPickerDelegate(private val onResult: (AudioFile?) -> Unit) :
    NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onResult((didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.let(::readAudio))
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readAudio(url: NSURL): AudioFile? {
    val data = NSData.dataWithContentsOfURL(url) ?: return null
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
    }
    val extension = url.pathExtension.orEmpty()
    val seconds = CMTimeGetSeconds(AVURLAsset.URLAssetWithURL(url, options = null).duration)
    return AudioFile(
        fileName = url.lastPathComponent ?: "audio",
        mimeType = UTType.typeWithFilenameExtension(extension)?.preferredMIMEType ?: "audio/mpeg",
        bytes = bytes,
        durationSeconds = seconds.takeIf { it.isFinite() && it > 0 }?.toInt(),
    )
}

private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) controller = controller.presentedViewController
    return controller
}
