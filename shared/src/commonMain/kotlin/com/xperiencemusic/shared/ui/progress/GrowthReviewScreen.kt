package com.xperiencemusic.shared.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.progress.GrowthReviewEvent
import com.xperiencemusic.shared.presentation.progress.GrowthReviewUiState
import com.xperiencemusic.shared.presentation.progress.GrowthReviewViewModel
import com.xperiencemusic.shared.presentation.progress.LevelRequestUi
import com.xperiencemusic.shared.presentation.progress.ObjectiveReviewUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.growth_review_confirm
import com.xperiencemusic.shared.resources.growth_review_empty_text
import com.xperiencemusic.shared.resources.growth_review_empty_title
import com.xperiencemusic.shared.resources.growth_review_objectives
import com.xperiencemusic.shared.resources.growth_review_request
import com.xperiencemusic.shared.resources.growth_review_requests
import com.xperiencemusic.shared.resources.growth_review_title
import com.xperiencemusic.shared.resources.review_comment
import com.xperiencemusic.shared.resources.review_comment_needed
import com.xperiencemusic.shared.resources.review_rework
import com.xperiencemusic.shared.resources.review_validate
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GrowthReviewScreen(onBack: () -> Unit, viewModel: GrowthReviewViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GrowthReviewScreenContent(state, viewModel::onEvent, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthReviewScreenContent(state: GrowthReviewUiState, onEvent: (GrowthReviewEvent) -> Unit, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.hasError) {
        if (state.hasError) {
            snackbar.showSnackbar(getString(Res.string.error_generic))
            onEvent(GrowthReviewEvent.ErrorShown)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.growth_review_title)) },
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
            if (state.requests.isEmpty() && state.objectives.isEmpty()) {
                item {
                    EmptyState(
                        icon = XmIcons.Medal,
                        title = stringResource(Res.string.growth_review_empty_title),
                        text = stringResource(Res.string.growth_review_empty_text),
                    )
                }
            }
            if (state.requests.isNotEmpty()) {
                item { Heading(stringResource(Res.string.growth_review_requests, state.requests.size)) }
                items(state.requests, key = { it.key }) { request ->
                    ReviewCard(
                        key = request.key,
                        name = request.memberName,
                        part = request.voicePart?.label() ?: request.instrument?.label(),
                        title = stringResource(Res.string.growth_review_request, request.target.label()),
                        description = null,
                        confirmLabel = stringResource(Res.string.growth_review_confirm),
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
            if (state.objectives.isNotEmpty()) {
                item { Heading(stringResource(Res.string.growth_review_objectives, state.objectives.size), Modifier.padding(top = Spacing.m)) }
                items(state.objectives, key = { it.key }) { objective ->
                    ReviewCard(
                        key = objective.key,
                        name = objective.memberName,
                        part = objective.instrument?.label(),
                        title = objective.title,
                        description = objective.criterion,
                        confirmLabel = stringResource(Res.string.review_validate),
                        state = state,
                        onEvent = onEvent,
                    )
                    com.xperiencemusic.shared.ui.learning.ObjectiveAudioReview(objective.memberId, objective.objectiveId)
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.semantics { heading() })
}

/** Who, what, a comment, and the two answers. Sending back needs a comment. */
@Composable
private fun ReviewCard(
    key: String,
    name: String,
    part: String?,
    title: String,
    description: String?,
    confirmLabel: String,
    state: GrowthReviewUiState,
    onEvent: (GrowthReviewEvent) -> Unit,
) {
    val isSaving = state.savingKey == key
    AppCard {
        Text(listOfNotNull(name, part).joinToString(" · "), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.xs))
        description?.takeIf { it.isNotEmpty() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = state.comments[key].orEmpty(),
            onValueChange = { onEvent(GrowthReviewEvent.CommentChanged(key, it)) },
            label = { Text(stringResource(Res.string.review_comment)) },
            isError = state.missingCommentKey == key,
            supportingText = if (state.missingCommentKey == key) {
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
                onClick = { onEvent(GrowthReviewEvent.Answered(key, validated = false)) },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(56.dp),
            ) { Text(stringResource(Res.string.review_rework)) }
            GradientButton(
                onClick = { onEvent(GrowthReviewEvent.Answered(key, validated = true)) },
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            ) { Text(confirmLabel) }
        }
    }
}

@Preview
@Composable
private fun GrowthReviewScreenPreview() {
    AppTheme {
        GrowthReviewScreenContent(
            GrowthReviewUiState(
                isLoading = false,
                requests = listOf(LevelRequestUi("l1", "r1", "Grâce", Instrument.SINGER, VoicePart.ALTO, Level.CONFIRMED)),
                objectives = listOf(
                    ObjectiveReviewUi("o1", "d", "Daniel", Instrument.PIANO, "piano-c1", "Les accords de la setlist", "Jouer tous les accords sans hésiter.", Level.CONFIRMED),
                ),
            ),
            onEvent = {}, onBack = {},
        )
    }
}
