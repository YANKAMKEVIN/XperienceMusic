package com.xperiencemusic.shared.ui.brand

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing

/** Compact brand block at the top of the sign-in and onboarding screens. */
@Composable
fun BrandHeader(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        XmMonogram(size = 88.dp, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        BrandWordmark(fontSize = 26.sp)
        BrandWaves(Modifier.fillMaxWidth().height(72.dp))
    }
}

/**
 * The one header of the four tabs: the monogram, the tab's title with an optional line under it,
 * and the tab's actions on the right as plain icons (menu, add). Left-aligned everywhere.
 */
@Composable
fun TabHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        XmMonogram(size = 36.dp)
        Column(Modifier.weight(1f).padding(start = Spacing.m)) {
            // "Ma progression" next to the streak pill: the title gives up a few points before it wraps.
            val titleStyle = MaterialTheme.typography.headlineSmall
            Text(
                title,
                style = titleStyle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = titleStyle.fontSize, stepSize = 1.sp),
                modifier = Modifier.semantics { heading() },
            )
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        actions()
    }
}

@Preview
@Composable
private fun TabHeaderPreview() = AppTheme {
    TabHeader("Semaine", subtitle = "du 2 au 8 novembre")
}

