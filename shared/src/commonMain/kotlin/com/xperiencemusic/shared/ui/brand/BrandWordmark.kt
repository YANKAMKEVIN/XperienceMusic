package com.xperiencemusic.shared.ui.brand

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.app_name
import com.xperiencemusic.shared.resources.brand_tagline
import com.xperiencemusic.shared.resources.brand_wordmark_bottom
import com.xperiencemusic.shared.resources.brand_wordmark_top
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing
import com.xperiencemusic.shared.ui.theme.WordmarkSize
import org.jetbrains.compose.resources.stringResource

/** « XPERIENCE » over a letter-spaced violet « M U S I C », optionally with the tagline. */
@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    size: WordmarkSize = WordmarkSize.Large,
    showTagline: Boolean = true,
) {
    val name = stringResource(Res.string.app_name)
    val fontSize = size.fontSize
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = name },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(
            stringResource(Res.string.brand_wordmark_top),
            style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Black, letterSpacing = 0.04.em),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(Res.string.brand_wordmark_bottom),
            style = TextStyle(
                brush = BrandColors.horizontalGradient,
                fontSize = fontSize * 0.62f,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.62.em,
            ),
        )
        if (showTagline) {
            Text(
                stringResource(Res.string.brand_tagline),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.18.em),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.ml),
            )
        }
    }
}
