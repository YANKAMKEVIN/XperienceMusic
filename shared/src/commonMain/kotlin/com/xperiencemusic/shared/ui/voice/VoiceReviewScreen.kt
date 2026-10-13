package com.xperiencemusic.shared.ui.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.voice.SubmissionUi
import com.xperiencemusic.shared.presentation.voice.VoiceReviewEvent
import com.xperiencemusic.shared.presentation.voice.VoiceReviewUiState
import com.xperiencemusic.shared.presentation.voice.VoiceReviewViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.review_comment
import com.xperiencemusic.shared.resources.review_comment_needed
import com.xperiencemusic.shared.resources.review_done_title
import com.xperiencemusic.shared.resources.review_empty_text
import com.xperiencemusic.shared.resources.review_empty_title
import com.xperiencemusic.shared.resources.review_rework
import com.xperiencemusic.shared.resources.review_title
import com.xperiencemusic.shared.resources.review_todo_title
import com.xperiencemusic.shared.resources.review_validate
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.ui.components.PlayPauseButton
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.progress.ExerciseStatusBadge
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VoiceReviewScreen(onBack: () -> Unit, viewModel: VoiceReviewViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VoiceReviewScreenContent(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceReviewScreenContent(state: VoiceReviewUiState, onEvent: (VoiceReviewEvent) -> Unit, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.hasError) {
        if (state.hasError) {
            snackbar.showSnackbar(getString(Res.string.error_generic))
            onEvent(VoiceReviewEvent.ErrorShown)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.review_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(0),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).imePadding(),
            contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            if (state.toReview.isEmpty()) {
                item {
                    EmptyState(
                        icon = XmIcons.Mic,
                        title = stringResource(Res.string.review_empty_title),
                        text = stringResource(Res.string.review_empty_text),
                    )
                }
            } else {
                item { SectionHeading(stringResource(Res.string.review_todo_title, state.toReview.size)) }
                items(state.toReview, key = { it.id }) { ToReviewCard(it, onEvent) }
            }
            if (state.reviewed.isNotEmpty()) {
                item { SectionHeading(stringResource(Res.string.review_done_title), Modifier.padding(top = Spacing.l)) }
                items(state.reviewed, key = { it.id }) { ReviewedCard(it, onEvent) }
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.semantics { heading() })
}

@Composable
private fun SubmitterRow(item: SubmissionUi, onEvent: (VoiceReviewEvent) -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PlayPauseButton(item.playback, itemLabel = item.memberName, onClick = { onEvent(VoiceReviewEvent.PlayClicked(item.id)) })
        Column(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
            Text(
                listOfNotNull(item.memberName, item.voicePart?.label()).joinToString(" · "),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "${item.songTitle} · ${item.duration} · ${item.date}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

@Composable
private fun ToReviewCard(item: SubmissionUi, onEvent: (VoiceReviewEvent) -> Unit) {
    AppCard {
        SubmitterRow(item, onEvent)
        OutlinedTextField(
            value = item.draft,
            onValueChange = { onEvent(VoiceReviewEvent.DraftChanged(item.id, it)) },
            label = { Text(stringResource(Res.string.review_comment)) },
            isError = item.needsComment,
            supportingText = if (item.needsComment) {
                { Text(stringResource(Res.string.review_comment_needed)) }
            } else {
                null
            },
            minLines = 2,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.m),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(top = Spacing.m)) {
            OutlinedButton(
                onClick = { onEvent(VoiceReviewEvent.ReworkClicked(item.id)) },
                enabled = !item.isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(56.dp),
            ) { Text(stringResource(Res.string.review_rework)) }
            GradientButton(
                onClick = { onEvent(VoiceReviewEvent.ValidateClicked(item.id)) },
                enabled = !item.isSaving,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(Res.string.review_validate)) }
        }
    }
}

@Composable
private fun ReviewedCard(item: SubmissionUi, onEvent: (VoiceReviewEvent) -> Unit) {
    AppCard {
        SubmitterRow(item, onEvent) { ExerciseStatusBadge(item.status) }
        item.feedback?.let {
            Text(
                "« $it »",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.s),
            )
        }
    }
}

@Preview
@Composable
private fun VoiceReviewPreview() {
    AppTheme {
        VoiceReviewScreenContent(
            VoiceReviewUiState(
                isLoading = false,
                toReview = listOf(
                    SubmissionUi("1", "Grâce", VoicePart.SOPRANO, "À toi la gloire", "lundi 12 oct.", "3:04", ExerciseStatus.SUBMITTED, null),
                ),
                reviewed = listOf(
                    SubmissionUi("2", "Daniel", VoicePart.TENOR, "Amazing Grace", "samedi 10 oct.", "3:21", ExerciseStatus.VALIDATED, "Très juste, bravo."),
                ),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
