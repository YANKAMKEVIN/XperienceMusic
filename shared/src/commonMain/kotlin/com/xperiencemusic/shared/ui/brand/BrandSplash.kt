package com.xperiencemusic.shared.ui.brand

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import com.xperiencemusic.shared.ui.theme.WordmarkSize

private val SPLASH_MONOGRAM_SIZE = 132.dp
private val SPLASH_WAVES_HEIGHT = 140.dp
private val LOADER_STROKE = 2.5.dp

/** Shown while the session is being restored: logo, wordmark, waves and a loader. */
@Composable
fun BrandSplash() {
    BrandBackground {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))
            XmMonogram(size = SPLASH_MONOGRAM_SIZE)
            Spacer(Modifier.height(Spacing.lxl))
            BrandWordmark(size = WordmarkSize.Splash)
            BrandWaves(Modifier.fillMaxWidth().height(SPLASH_WAVES_HEIGHT))
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator(
                strokeWidth = LOADER_STROKE,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(Sizes.iconXxl),
            )
            Spacer(Modifier.height(Sizes.touchTarget))
        }
    }
}

@Preview
@Composable
private fun BrandSplashPreview() {
    AppTheme { BrandSplash() }
}
