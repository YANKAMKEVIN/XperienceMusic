package com.xperiencemusic.shared.ui.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.ui.theme.BrandColors

/** The screen's main action, in the brand gradient. One per screen at most. */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .then(if (enabled) Modifier.background(BrandColors.horizontalGradient, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp),
                content = content,
            )
        }
    }
}

/** A key ("Sol", "Mim") in a small tinted pill. */
@Composable
fun KeyPill(label: String, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .alpha(if (dimmed) 0.6f else 1f)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
