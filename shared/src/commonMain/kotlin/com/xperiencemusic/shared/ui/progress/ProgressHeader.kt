package com.xperiencemusic.shared.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.presentation.progress.ProgressUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.growth_validate_open
import com.xperiencemusic.shared.resources.progress_streak
import com.xperiencemusic.shared.resources.review_open
import com.xperiencemusic.shared.resources.tab_progress
import com.xperiencemusic.shared.ui.brand.TabHeader
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val STREAK_PILL_PADDING = PaddingValues(horizontal = Spacing.m, vertical = 7.dp)

@Composable
internal fun ProgressHeader(state: ProgressUiState, modifier: Modifier = Modifier) {
    TabHeader(
        title = stringResource(Res.string.tab_progress),
        subtitle = listOfNotNull(state.name.takeIf { it.isNotBlank() }, state.voicePart?.label() ?: state.instrument?.label(), state.level?.label()).joinToString(" · "),
        modifier = modifier,
    ) {
        if (state.hasPath) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(Sizes.hairline, MaterialTheme.colorScheme.outline, CircleShape)
                    .padding(STREAK_PILL_PADDING),
            ) {
                Icon(XmIcons.Flame, contentDescription = null, tint = if (state.streakDays > 0) AppTheme.colors.warning else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Sizes.iconXs))
                Text(pluralStringResource(Res.plurals.progress_streak, state.streakDays, state.streakDays), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = Spacing.xsm))
            }
        }
    }
}

/** For the Admin++: what waits for them, only when something does. */
@Composable
internal fun AdminStrip(state: ProgressUiState, onReview: () -> Unit, onValidate: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        if (state.toReviewCount > 0) AdminPill(stringResource(Res.string.review_open, state.toReviewCount), onReview)
        if (state.toValidateCount > 0) AdminPill(stringResource(Res.string.growth_validate_open, state.toValidateCount), onValidate)
    }
}

@Composable
private fun AdminPill(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = Sizes.compactTarget)
            .clip(CircleShape)
            .border(Sizes.hairline, BrandColors.gradient, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.ml),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
