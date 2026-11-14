package com.xperiencemusic.shared.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Radius
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing

/** The square tile at the start of a choice card (an icon or the level bars). */
private val TILE_SIZE = 52.dp
/** The level bars rise from [LEVEL_BAR_BASE] by [LEVEL_BAR_STEP] each. */
private val LEVEL_BAR_WIDTH = 7.dp
private val LEVEL_BAR_BASE = 10.dp
private val LEVEL_BAR_STEP = 8.dp
private val VOICE_COLUMN_MAX_HEIGHT = 160.dp
private val VOICE_DOT_SIZE = 10.dp

/** A big tappable answer: icon tile, title, one line of explanation; the gradient marks the choice. */
@Composable
fun ChoiceCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.cardLarge)
    // The tint and the check ease in, so a choice is felt rather than flashed.
    val background by animateColorAsState(
        if (isSelected) colors.primaryContainer.copy(alpha = 0.35f) else colors.surfaceContainer,
        label = "choice-background",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(if (isSelected) Sizes.strokeMedium else Sizes.hairline, if (isSelected) BrandColors.gradient else SolidColor(colors.outlineVariant), shape)
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(Spacing.m),
    ) {
        leading()
        Column(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        // The check keeps its room while hidden, so the description does not reflow when it appears.
        val check by animateFloatAsState(if (isSelected) 1f else 0f, label = "choice-check")
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(Sizes.icon).graphicsLayer { alpha = check; scaleX = check; scaleY = check },
        )
    }
}

@Composable
fun IconTile(icon: ImageVector, isSelected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(TILE_SIZE)
            .clip(RoundedCornerShape(Radius.card))
            .background(if (isSelected) BrandColors.gradient else SolidColor(colors.surfaceContainerHigh)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) colors.onPrimary else colors.primary, modifier = Modifier.size(Sizes.iconXl))
    }
}

/** Rising bars, [filled] of [total]: the level at a glance. */
@Composable
fun LevelBars(filled: Int, total: Int, isSelected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(TILE_SIZE).clip(RoundedCornerShape(Radius.card)).background(colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.Bottom) {
            repeat(total) { index ->
                Box(
                    Modifier
                        .width(LEVEL_BAR_WIDTH)
                        .height(LEVEL_BAR_BASE + LEVEL_BAR_STEP * index)
                        .clip(RoundedCornerShape(Radius.bar))
                        .background(
                            when {
                                index >= filled -> SolidColor(colors.outline)
                                isSelected -> BrandColors.gradient
                                else -> SolidColor(colors.primary)
                            },
                        ),
                )
            }
        }
    }
}

/**
 * The four voices as columns, highest to lowest: a soprano reads tall, a bass short. [heightFraction]
 * of the tallest column.
 */
@Composable
fun VoiceColumn(
    label: String,
    heightFraction: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = VOICE_COLUMN_MAX_HEIGHT,
) {
    val colors = MaterialTheme.colorScheme
    val height by animateDpAsState(maxHeight * heightFraction * (if (isSelected) 1f else 0.92f), label = "voice")
    val labelColor by animateColorAsState(if (isSelected) colors.onSurface else colors.onSurfaceVariant, label = "voiceLabel")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton),
    ) {
        Box(Modifier.height(maxHeight), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(Radius.card))
                    .background(if (isSelected) BrandColors.gradient else SolidColor(colors.surfaceContainerHigh))
                    .border(Sizes.hairline, if (isSelected) SolidColor(colors.primary) else SolidColor(colors.outlineVariant), RoundedCornerShape(Radius.card)),
            )
            if (isSelected) {
                Box(
                    Modifier.padding(bottom = Spacing.sm).size(VOICE_DOT_SIZE).clip(CircleShape).background(colors.onPrimary),
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.s),
        )
    }
}
