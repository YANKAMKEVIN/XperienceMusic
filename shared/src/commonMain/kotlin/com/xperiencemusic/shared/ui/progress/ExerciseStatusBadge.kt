package com.xperiencemusic.shared.ui.progress

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.status_submitted
import com.xperiencemusic.shared.resources.status_to_rework
import com.xperiencemusic.shared.resources.status_todo
import com.xperiencemusic.shared.resources.status_validated
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExerciseStatusBadge(status: ExerciseStatus, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (label, color) = when (status) {
        ExerciseStatus.TODO -> Res.string.status_todo to colors.onSurfaceVariant
        ExerciseStatus.SUBMITTED -> Res.string.status_submitted to colors.secondary
        ExerciseStatus.VALIDATED -> Res.string.status_validated to colors.tertiary
        ExerciseStatus.TO_REWORK -> Res.string.status_to_rework to colors.primary
    }
    Surface(
        shape = RoundedCornerShape(Spacing.s),
        color = colors.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xxs),
        )
    }
}
