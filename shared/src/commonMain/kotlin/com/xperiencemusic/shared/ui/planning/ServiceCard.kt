package com.xperiencemusic.shared.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.ServiceKind
import com.xperiencemusic.shared.presentation.planning.MyAnswer
import com.xperiencemusic.shared.presentation.planning.ServiceCardUi
import com.xperiencemusic.shared.presentation.planning.ServingUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.planning_complete
import com.xperiencemusic.shared.resources.planning_friday
import com.xperiencemusic.shared.resources.planning_missing
import com.xperiencemusic.shared.resources.planning_nobody
import com.xperiencemusic.shared.resources.planning_not_available
import com.xperiencemusic.shared.resources.planning_sunday
import com.xperiencemusic.shared.resources.planning_to_answer
import com.xperiencemusic.shared.resources.planning_today
import com.xperiencemusic.shared.resources.planning_you_serve
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.PillPadding
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow

private val DATE_COLUMN_WIDTH = 48.dp

/** A Friday or Sunday: big date on the left, the team, what is missing, and the member's answer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ServiceCard(card: ServiceCardUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(onClick = onClick, modifier = modifier.alpha(if (card.isPast) 0.55f else 1f)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(DATE_COLUMN_WIDTH)) {
                Text(card.weekday, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(card.day.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f).padding(start = Spacing.s)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (card.kind == ServiceKind.FRIDAY) Res.string.planning_friday else Res.string.planning_sunday),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (card.isToday) TodayPill(Modifier.padding(start = Spacing.s))
                    Spacer(Modifier.weight(1f))
                    if (card.showsAnswer) AnswerBadge(card.myAnswer)
                }
                if (card.serving.isEmpty()) {
                    Text(
                        stringResource(Res.string.planning_nobody),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.padding(top = Spacing.s),
                    ) { card.serving.forEach { PersonPill(it) } }
                }
                if (!card.isPast) MissingLine(card.missing, Modifier.padding(top = Spacing.s))
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
internal fun MissingLine(missing: List<Instrument>, modifier: Modifier = Modifier) {
    if (missing.isEmpty()) {
        Text(stringResource(Res.string.planning_complete), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary, modifier = modifier)
    } else {
        Text(
            stringResource(Res.string.planning_missing, missing.map { it.label().lowercase() }.joinToString(", ")),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.warning,
            modifier = modifier,
        )
    }
}

@Composable
private fun TodayPill(modifier: Modifier = Modifier) {
    Text(
        stringResource(Res.string.planning_today),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = BrandColors.onGradient,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.background(BrandColors.horizontalGradient, CircleShape).padding(PillPadding.tag),
    )
}

@Composable
private fun AnswerBadge(answer: MyAnswer) {
    val (label, color) = when (answer) {
        MyAnswer.SERVES -> stringResource(Res.string.planning_you_serve) to MaterialTheme.colorScheme.tertiary
        MyAnswer.NOT_AVAILABLE -> stringResource(Res.string.planning_not_available) to MaterialTheme.colorScheme.onSurfaceVariant
        MyAnswer.UNANSWERED -> stringResource(Res.string.planning_to_answer) to MaterialTheme.colorScheme.primary
    }
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), CircleShape)
            .padding(PillPadding.tag),
    )
}

/** "Kevin · ténor", the signed-in member highlighted. */
@Composable
internal fun PersonPill(person: ServingUi) {
    val part = person.voicePart?.label()?.lowercase() ?: person.instrument?.label()?.lowercase()
    val color = if (person.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Text(
        listOfNotNull(person.name, part).joinToString(" · "),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(
                if (person.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                CircleShape,
            )
            .padding(PillPadding.badge),
    )
}
