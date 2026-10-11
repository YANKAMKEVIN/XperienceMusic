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
import androidx.compose.ui.unit.sp
import com.xperiencemusic.shared.ui.theme.AppTheme

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
            XmMonogram(size = 132.dp)
            Spacer(Modifier.height(20.dp))
            BrandWordmark(fontSize = 34.sp)
            BrandWaves(Modifier.fillMaxWidth().height(140.dp))
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator(
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Preview
@Composable
private fun BrandSplashPreview() {
    AppTheme { BrandSplash() }
}
