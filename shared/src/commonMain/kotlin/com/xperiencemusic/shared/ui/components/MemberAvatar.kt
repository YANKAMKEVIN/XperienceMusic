package com.xperiencemusic.shared.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xperiencemusic.shared.domain.model.Avatar
import com.xperiencemusic.shared.domain.model.AvatarPreset
import com.xperiencemusic.shared.domain.repository.AvatarStorage
import com.xperiencemusic.shared.presentation.common.MemberAvatarUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.avatar_bass_boy_1
import com.xperiencemusic.shared.resources.avatar_bass_boy_2
import com.xperiencemusic.shared.resources.avatar_bass_girl_1
import com.xperiencemusic.shared.resources.avatar_bass_girl_2
import com.xperiencemusic.shared.resources.avatar_drums_boy_1
import com.xperiencemusic.shared.resources.avatar_drums_boy_2
import com.xperiencemusic.shared.resources.avatar_drums_girl_1
import com.xperiencemusic.shared.resources.avatar_drums_girl_2
import com.xperiencemusic.shared.resources.avatar_piano_boy_1
import com.xperiencemusic.shared.resources.avatar_piano_boy_2
import com.xperiencemusic.shared.resources.avatar_piano_girl_1
import com.xperiencemusic.shared.resources.avatar_piano_girl_2
import com.xperiencemusic.shared.resources.avatar_singer_boy_1
import com.xperiencemusic.shared.resources.avatar_singer_boy_2
import com.xperiencemusic.shared.resources.avatar_singer_girl_1
import com.xperiencemusic.shared.resources.avatar_singer_girl_2
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

/**
 * A member in a circle: their photo, the avatar they picked, or their initials on the brand
 * gradient (also shown while a photo loads or when it cannot be fetched offline). Decorative: the
 * caller names the member next to it or in the clickable's label.
 */
@Composable
fun MemberAvatar(member: MemberAvatarUi, size: Dp, modifier: Modifier = Modifier) {
    val circle = modifier.size(size).clip(CircleShape)
    when (val avatar = member.avatar) {
        is Avatar.Preset -> PresetImage(avatar.preset, circle)
        is Avatar.Photo -> {
            val photo = rememberAvatarPhoto(avatar.path)
            if (photo != null) {
                Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = circle)
            } else {
                Initials(member.initials, size, circle)
            }
        }
        null -> Initials(member.initials, size, circle)
    }
}

@Composable
fun PresetImage(preset: AvatarPreset, modifier: Modifier = Modifier) {
    Image(painterResource(preset.drawable()), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}

@Composable
private fun rememberAvatarPhoto(path: String): ImageBitmap? {
    val storage = koinInject<AvatarStorage>()
    val photo by produceState<ImageBitmap?>(null, path) {
        value = storage.download(path)?.let { bytes -> runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
    }
    return photo
}

@Composable
private fun Initials(initials: String, size: Dp, modifier: Modifier) {
    Box(modifier.background(BrandColors.gradient), contentAlignment = Alignment.Center) {
        Text(
            initials,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            // About 40 % of the circle, whatever its size.
            fontSize = (size.value * 0.4f).sp,
        )
    }
}

private fun AvatarPreset.drawable(): DrawableResource = when (this) {
    AvatarPreset.SINGER_BOY_1 -> Res.drawable.avatar_singer_boy_1
    AvatarPreset.SINGER_BOY_2 -> Res.drawable.avatar_singer_boy_2
    AvatarPreset.SINGER_GIRL_1 -> Res.drawable.avatar_singer_girl_1
    AvatarPreset.SINGER_GIRL_2 -> Res.drawable.avatar_singer_girl_2
    AvatarPreset.PIANO_BOY_1 -> Res.drawable.avatar_piano_boy_1
    AvatarPreset.PIANO_BOY_2 -> Res.drawable.avatar_piano_boy_2
    AvatarPreset.PIANO_GIRL_1 -> Res.drawable.avatar_piano_girl_1
    AvatarPreset.PIANO_GIRL_2 -> Res.drawable.avatar_piano_girl_2
    AvatarPreset.BASS_BOY_1 -> Res.drawable.avatar_bass_boy_1
    AvatarPreset.BASS_BOY_2 -> Res.drawable.avatar_bass_boy_2
    AvatarPreset.BASS_GIRL_1 -> Res.drawable.avatar_bass_girl_1
    AvatarPreset.BASS_GIRL_2 -> Res.drawable.avatar_bass_girl_2
    AvatarPreset.DRUMS_BOY_1 -> Res.drawable.avatar_drums_boy_1
    AvatarPreset.DRUMS_BOY_2 -> Res.drawable.avatar_drums_boy_2
    AvatarPreset.DRUMS_GIRL_1 -> Res.drawable.avatar_drums_girl_1
    AvatarPreset.DRUMS_GIRL_2 -> Res.drawable.avatar_drums_girl_2
}

@Preview
@Composable
private fun MemberAvatarPreview() = AppTheme {
    MemberAvatar(MemberAvatarUi("Grâce Mbala", Avatar.Preset(AvatarPreset.SINGER_GIRL_1)), 56.dp)
}

@Preview
@Composable
private fun MemberAvatarInitialsPreview() = AppTheme {
    MemberAvatar(MemberAvatarUi("Grâce Mbala", null), 56.dp)
}
