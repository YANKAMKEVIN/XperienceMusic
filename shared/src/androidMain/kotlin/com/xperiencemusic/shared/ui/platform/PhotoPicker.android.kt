package com.xperiencemusic.shared.ui.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

@Composable
actual fun rememberPhotoPicker(onResult: (ByteArray?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            currentOnResult.value(null)
        } else {
            scope.launch {
                val jpeg = withContext(Dispatchers.IO) { runCatching { squareJpeg(context, uri) }.getOrNull() }
                currentOnResult.value(jpeg)
            }
        }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

private fun squareJpeg(context: Context, uri: Uri): ByteArray? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val shortSide = min(bounds.outWidth, bounds.outHeight)
    if (shortSide <= 0) return null
    // Decode no larger than needed: a 12 MP photo would otherwise take ~50 MB of memory.
    var sample = 1
    while (shortSide / (sample * 2) >= AVATAR_PHOTO_SIZE_PX) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null
    val degrees = resolver.openInputStream(uri)?.use { exifRotation(ExifInterface(it)) } ?: 0f

    val side = min(decoded.width, decoded.height)
    val scale = AVATAR_PHOTO_SIZE_PX.toFloat() / side
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postRotate(degrees)
    }
    val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side, matrix, true)
    return ByteArrayOutputStream().use { out ->
        square.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        out.toByteArray()
    }
}

/** Phones store photos sideways with an orientation tag; the picture is turned upright here. */
private fun exifRotation(exif: ExifInterface): Float =
    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

private const val JPEG_QUALITY = 85
