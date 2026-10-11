package com.xperiencemusic.shared.ui.brand

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compact brand block at the top of the sign-in and onboarding screens. */
@Composable
fun BrandHeader(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        XmMonogram(size = 88.dp, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        BrandWordmark(fontSize = 26.sp)
        BrandWaves(Modifier.fillMaxWidth().height(72.dp))
    }
}
