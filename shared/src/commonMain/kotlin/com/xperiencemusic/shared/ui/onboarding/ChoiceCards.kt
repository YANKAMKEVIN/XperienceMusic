package com.xperiencemusic.shared.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing

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
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) colors.primaryContainer.copy(alpha = 0.35f) else colors.surfaceContainer)
            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) BrandColors.gradient else SolidColor(colors.outlineVariant), shape)
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(Spacing.m),
    ) {
        leading()
        Column(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        if (isSelected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.primary)
    }
}

@Composable
fun IconTile(icon: ImageVector, isSelected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) BrandColors.gradient else SolidColor(colors.surfaceContainerHigh)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) colors.onPrimary else colors.primary, modifier = Modifier.size(26.dp))
    }
}

/** Rising bars, [filled] of [total]: the level at a glance. */
@Composable
fun LevelBars(filled: Int, total: Int, isSelected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            repeat(total) { index ->
                Box(
                    Modifier
                        .width(7.dp)
                        .height(10.dp + 8.dp * index)
                        .clip(RoundedCornerShape(3.dp))
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
    maxHeight: Dp = 160.dp,
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) BrandColors.gradient else SolidColor(colors.surfaceContainerHigh))
                    .border(1.dp, if (isSelected) SolidColor(colors.primary) else SolidColor(colors.outlineVariant), RoundedCornerShape(16.dp)),
            )
            if (isSelected) {
                Box(
                    Modifier.padding(bottom = 10.dp).size(10.dp).clip(CircleShape).background(colors.onPrimary),
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
