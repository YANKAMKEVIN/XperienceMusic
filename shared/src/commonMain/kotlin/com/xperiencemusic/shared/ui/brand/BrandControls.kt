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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import com.xperiencemusic.shared.ui.theme.PillPadding
import com.xperiencemusic.shared.ui.theme.Radius
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing

/** Darkens the gradient while the button is pressed (the gradient hides Material's ripple). */
private val PRESSED_SCRIM = Color.Black.copy(alpha = 0.16f)

/**
 * The screen's main action, in the brand gradient. One per screen at most. The gradient hides
 * Material's ripple, so a press shrinks the button a little and darkens it instead.
 */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Radius.card)
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, tween(120), label = "gradient-press")
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = BrandColors.onGradient,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minHeight = Sizes.rowMin),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Sizes.rowMin)
                .then(if (enabled) Modifier.background(BrandColors.horizontalGradient, shape) else Modifier)
                .then(if (isPressed) Modifier.background(PRESSED_SCRIM, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.lxl),
                content = content,
            )
        }
    }
}

/** A key ("Sol", "Mim") in a small tinted pill, always on one line. */
@Composable
fun KeyPill(label: String, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .alpha(if (dimmed) 0.6f else 1f)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(PillPadding.tag),
    )
}
