package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy
import kotlin.math.min

@Composable
actual fun rememberPhotoPicker(onResult: (ByteArray?) -> Unit): () -> Unit {
    val currentOnResult = rememberUpdatedState(onResult)
    // UIKit only keeps a weak reference to the delegate; remember keeps it alive.
    val delegate = remember { PhotoPickerDelegate { currentOnResult.value(it) } }
    return remember(delegate) {
        {
            val configuration = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }
            val picker = PHPickerViewController(configuration)
            picker.delegate = delegate
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class PhotoPickerDelegate(private val onResult: (ByteArray?) -> Unit) :
    NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
        if (provider == null) {
            onResult(null)
            return
        }
        provider.loadDataRepresentationForTypeIdentifier(IMAGE_TYPE) { data, _ ->
            val jpeg = data?.let(UIImage::imageWithData)?.let(::squareJpeg)
            dispatch_async(dispatch_get_main_queue()) { onResult(jpeg) }
        }
    }
}

/** Draws the centre square upright (UIImage applies the photo's orientation) at the avatar size. */
@OptIn(ExperimentalForeignApi::class)
private fun squareJpeg(image: UIImage): ByteArray? {
    val (width, height) = image.size.useContents { width to height }
    val side = min(width, height)
    if (side <= 0.0) return null
    val target = AVATAR_PHOTO_SIZE_PX.toDouble()
    val scale = target / side
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { this.scale = 1.0 }
    val rendered = UIGraphicsImageRenderer(size = CGSizeMake(target, target), format = format).imageWithActions { _ ->
        val drawWidth = width * scale
        val drawHeight = height * scale
        image.drawInRect(CGRectMake((target - drawWidth) / 2, (target - drawHeight) / 2, drawWidth, drawHeight))
    }
    return UIImageJPEGRepresentation(rendered, JPEG_QUALITY)?.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}

private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) controller = controller.presentedViewController
    return controller
}

private const val IMAGE_TYPE = "public.image"
private const val JPEG_QUALITY = 0.85
