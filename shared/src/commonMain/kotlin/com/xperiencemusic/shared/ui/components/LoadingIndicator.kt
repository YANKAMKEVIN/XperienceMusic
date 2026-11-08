package com.xperiencemusic.shared.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * The wait for a screen's content: nothing for a short moment, then a spinner that fades in, so a
 * quick load never flashes one.
 */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(300)
        shown.animateTo(1f, tween(300))
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.graphicsLayer { alpha = shown.value })
    }
}
