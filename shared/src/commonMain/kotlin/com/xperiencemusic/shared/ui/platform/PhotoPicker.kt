package com.xperiencemusic.shared.ui.platform

import androidx.compose.runtime.Composable

/** Side of the square photo sent for an avatar: sharp at 120 dp on any phone, about 50 KB. */
const val AVATAR_PHOTO_SIZE_PX = 512

/**
 * Returns a function that opens the system photo picker. [onResult] receives the picked photo,
 * upright, cropped to its centre square and scaled to [AVATAR_PHOTO_SIZE_PX] as JPEG bytes; null
 * when the user cancels or the image cannot be read.
 */
@Composable
expect fun rememberPhotoPicker(onResult: (ByteArray?) -> Unit): () -> Unit
