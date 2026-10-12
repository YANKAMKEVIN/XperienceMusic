package com.xperiencemusic.shared.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.resources.progress_exercises_title
import com.xperiencemusic.shared.resources.progress_hello
import com.xperiencemusic.shared.resources.progress_start
import com.xperiencemusic.shared.ui.brand.BrandWaves
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.theme.BrandColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.presentation.progress.ExerciseCheckUi
import com.xperiencemusic.shared.presentation.progress.ExerciseUi
import com.xperiencemusic.shared.presentation.progress.ProgressUiState
import com.xperiencemusic.shared.presentation.progress.ProgressViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.members_open
import com.xperiencemusic.shared.resources.progress_check_hold
import com.xperiencemusic.shared.resources.progress_check_review
import com.xperiencemusic.shared.resources.progress_done
import com.xperiencemusic.shared.resources.progress_musicians_soon_text
import com.xperiencemusic.shared.resources.progress_musicians_soon_title
import com.xperiencemusic.shared.resources.progress_profile
import com.xperiencemusic.shared.resources.progress_title
import com.xperiencemusic.shared.resources.session_sign_out
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProgressScreen(
    onExerciseClick: (String) -> Unit,
    onManageMembers: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ProgressViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgressScreenContent(
        state = state,
        onExerciseClick = onExerciseClick,
        onManageMembers = onManageMembers,
        onSignOut = onSignOut,
    )
}

@Composable
fun ProgressScreenContent(
    state: ProgressUiState,
    onExerciseClick: (String) -> Unit,
    onManageMembers: () -> Unit,
    onSignOut: () -> Unit,
) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        item { ProgressHeader(state) }
        if (!state.hasPath) {
            item {
                EmptyState(
                    icon = XmIcons.Piano,
                    title = stringResource(Res.string.progress_musicians_soon_title),
                    text = stringResource(Res.string.progress_musicians_soon_text),
                )
            }
        } else {
            item { PathCard(state.validatedCount, state.exercises.size) }
            item {
                Text(
                    stringResource(Res.string.progress_exercises_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
                )
            }
            items(state.exercises, key = { it.id }) { ExerciseRow(it, onClick = { onExerciseClick(it.id) }) }
            state.nextExerciseId?.let { next ->
                item {
                    GradientButton(onClick = { onExerciseClick(next) }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.s)) {
                        Text(stringResource(Res.string.progress_start), style = MaterialTheme.typography.titleMedium)
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(start = Spacing.s).size(20.dp),
                        )
                    }
                }
            }
        }
        if (state.canManageMembers) {
            item {
                OutlinedButton(onClick = onManageMembers, modifier = Modifier.fillMaxWidth().padding(top = Spacing.l)) {
                    Text(stringResource(Res.string.members_open))
                }
            }
        }
        item {
            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().padding(top = Spacing.l)) {
                Text(stringResource(Res.string.session_sign_out))
            }
        }
    }
}

@Composable
private fun ProgressHeader(state: ProgressUiState) {
    Column {
        Text(
            stringResource(Res.string.progress_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(Res.string.progress_hello, state.name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.m)) {
            val pills = listOfNotNull(
                state.voicePart?.label() ?: state.instrument?.label(),
                state.level?.label(),
            )
            pills.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(end = Spacing.s)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .padding(horizontal = Spacing.m, vertical = Spacing.xs),
                )
            }
            BrandWaves(Modifier.weight(1f).height(36.dp))
        }
    }
}

/** How far along the singer's path is, with the brand gradient as the bar. */
@Composable
private fun PathCard(validated: Int, total: Int) {
    AppCard {
        Text(stringResource(Res.string.progress_done, validated, total), style = MaterialTheme.typography.titleMedium)
        Box(
            Modifier
                .padding(top = Spacing.m)
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(if (total == 0) 0f else validated.toFloat() / total)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(BrandColors.horizontalGradient),
            )
        }
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseUi, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (exercise.check is ExerciseCheckUi.HoldNote) XmIcons.Notes else XmIcons.Bars,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
                Text(exercise.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    when (val check = exercise.check) {
                        is ExerciseCheckUi.HoldNote -> stringResource(Res.string.progress_check_hold, check.noteLabel, check.seconds)
                        ExerciseCheckUi.Review -> stringResource(Res.string.progress_check_review)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.xs),
                )
                ExerciseStatusBadge(exercise.status)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun ProgressScreenSingerPreview() {
    AppTheme {
        ProgressScreenContent(
            ProgressUiState(
                isLoading = false,
                name = "Grâce",
                instrument = Instrument.SINGER,
                level = Level.BEGINNER,
                hasPath = true,
                exercises = listOf(
                    ExerciseUi(
                        "1", 1, "Tenir ta note", "Chante ta note de confort sur « a ».",
                        ExerciseCheckUi.HoldNote("Ré4", 5),
                        ExerciseStatus.VALIDATED,
                    ),
                    ExerciseUi(
                        "2", 2, "Gamme majeure", "Monte puis descends la gamme.",
                        ExerciseCheckUi.Review,
                        ExerciseStatus.TODO,
                    ),
                ),
                validatedCount = 1,
                voicePart = VoicePart.ALTO,
                nextExerciseId = "2",
            ),
            onExerciseClick = {},
            onManageMembers = {},
            onSignOut = {},
        )
    }
}

@Preview
@Composable
private fun ProgressScreenMusicianPreview() {
    AppTheme {
        ProgressScreenContent(
            ProgressUiState(isLoading = false, name = "Daniel", instrument = Instrument.PIANO, level = Level.PRO),
            onExerciseClick = {},
            onManageMembers = {},
            onSignOut = {},
        )
    }
}
