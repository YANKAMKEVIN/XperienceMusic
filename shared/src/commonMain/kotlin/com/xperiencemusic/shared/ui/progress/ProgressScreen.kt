package com.xperiencemusic.shared.ui.progress

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
import androidx.compose.material3.Text
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
import com.xperiencemusic.shared.resources.progress_check_hold
import com.xperiencemusic.shared.resources.progress_check_review
import com.xperiencemusic.shared.resources.progress_done
import com.xperiencemusic.shared.resources.progress_musicians_soon_text
import com.xperiencemusic.shared.resources.progress_musicians_soon_title
import com.xperiencemusic.shared.resources.progress_profile
import com.xperiencemusic.shared.resources.progress_title
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgressScreenContent(state = state)
}

@Composable
fun ProgressScreenContent(state: ProgressUiState) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        item {
            Text(stringResource(Res.string.progress_title), style = MaterialTheme.typography.headlineMedium)
            val instrument = state.instrument?.label().orEmpty()
            val level = state.level?.label().orEmpty()
            Text(
                state.name + " · " + stringResource(Res.string.progress_profile, instrument, level),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!state.hasPath) {
            item {
                AppCard {
                    Text(stringResource(Res.string.progress_musicians_soon_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(Res.string.progress_musicians_soon_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.s),
                    )
                }
            }
        } else {
            item {
                val total = state.exercises.size
                Text(stringResource(Res.string.progress_done, state.validatedCount, total))
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else state.validatedCount.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
                )
            }
            items(state.exercises, key = { it.id }) { ExerciseRow(it) }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseUi) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${exercise.position}. ${exercise.title}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            ExerciseStatusBadge(exercise.status)
        }
        Text(
            exercise.instructions,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = Spacing.s),
        )
        Text(
            when (val check = exercise.check) {
                is ExerciseCheckUi.HoldNote -> stringResource(Res.string.progress_check_hold, check.noteLabel, check.seconds)
                ExerciseCheckUi.Review -> stringResource(Res.string.progress_check_review)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = Spacing.s),
        )
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
            ),
        )
    }
}

@Preview
@Composable
private fun ProgressScreenMusicianPreview() {
    AppTheme {
        ProgressScreenContent(
            ProgressUiState(isLoading = false, name = "Daniel", instrument = Instrument.PIANO, level = Level.PRO),
        )
    }
}
