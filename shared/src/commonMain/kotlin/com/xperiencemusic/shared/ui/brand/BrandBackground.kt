package com.xperiencemusic.shared.ui.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Palette

/** Night background with a faint violet glow at the top, used behind the welcome screens. */
@Composable
fun BrandBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Palette.Night)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(BrandColors.Indigo.copy(alpha = 0.24f), Color.Transparent),
                        center = Offset(size.width / 2, size.height * 0.18f),
                        radius = size.maxDimension * 0.6f,
                    ),
                )
            },
        content = content,
    )
}
