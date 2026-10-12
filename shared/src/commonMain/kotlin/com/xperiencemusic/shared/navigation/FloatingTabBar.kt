package com.xperiencemusic.shared.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing

/** One tab of the floating bar. */
data class FloatingTab(val label: String, val icon: ImageVector, val isSelected: Boolean, val onClick: () -> Unit)

/**
 * The tabs in a pill that floats above the bottom edge. The selected tab widens into a gradient
 * capsule with its name; the others show their icon only. Changes spring from one tab to the next.
 */
@Composable
fun FloatingTabBar(tabs: List<FloatingTab>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .padding(horizontal = Spacing.s)
            .selectableGroup(),
    ) {
        tabs.forEach { tab -> TabItem(tab) }
    }
}

@Composable
private fun TabItem(tab: FloatingTab) {
    val colors = MaterialTheme.colorScheme
    val content by animateColorAsState(if (tab.isSelected) Color.White else colors.onSurfaceVariant, label = "tabContent")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(if (tab.isSelected) BrandColors.horizontalGradient else SolidColor(Color.Transparent))
            .selectable(selected = tab.isSelected, onClick = tab.onClick, role = Role.Tab)
            .padding(horizontal = if (tab.isSelected) 18.dp else 16.dp),
    ) {
        Icon(tab.icon, contentDescription = if (tab.isSelected) null else tab.label, tint = content, modifier = Modifier.size(22.dp))
        AnimatedVisibility(
            visible = tab.isSelected,
            enter = expandHorizontally(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
        ) {
            Box(Modifier.padding(start = Spacing.s)) {
                Text(tab.label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
