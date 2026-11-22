package com.xperiencemusic.shared.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.HelpAudience
import com.xperiencemusic.shared.domain.model.HelpBlock
import com.xperiencemusic.shared.domain.model.HelpPage
import com.xperiencemusic.shared.domain.model.HelpSection
import com.xperiencemusic.shared.domain.model.HelpSpan
import com.xperiencemusic.shared.domain.model.HelpText
import com.xperiencemusic.shared.presentation.help.HelpEvent
import com.xperiencemusic.shared.presentation.help.HelpUiState
import com.xperiencemusic.shared.presentation.help.HelpViewModel
import com.xperiencemusic.shared.presentation.help.deriveHelpState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.help_bullet
import com.xperiencemusic.shared.resources.help_step_number
import com.xperiencemusic.shared.resources.help_tab_leaders
import com.xperiencemusic.shared.resources.help_tab_members
import com.xperiencemusic.shared.resources.help_title
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.ChoiceChips
import com.xperiencemusic.shared.ui.components.LoadingIndicator
import com.xperiencemusic.shared.ui.components.SectionTitle
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HelpScreen(onBack: () -> Unit, viewModel: HelpViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HelpScreenContent(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

/** The page of `docs/aide` for the member's role; leaders switch between theirs and the members' one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreenContent(state: HelpUiState, onEvent: (HelpEvent) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        val page = state.page
        if (state.isLoading || page == null) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            if (state.showsTabs) {
                item(key = "tabs") {
                    ChoiceChips(
                        options = state.audiences,
                        selected = state.selected,
                        label = { audienceLabel(it) },
                        onSelect = { onEvent(HelpEvent.AudienceSelected(it)) },
                    )
                }
            }
            if (page.intro.isNotEmpty()) {
                item(key = "${page.audience}-intro") { BlockColumn { page.intro.forEach { Block(it) } } }
            }
            items(page.sections, key = { "${page.audience}-${it.title}" }) { section -> Section(section) }
        }
    }
}

@Composable
private fun audienceLabel(audience: HelpAudience): String = stringResource(
    when (audience) {
        HelpAudience.MEMBERS -> Res.string.help_tab_members
        HelpAudience.LEADERS -> Res.string.help_tab_leaders
    },
)

@Composable
private fun Section(section: HelpSection) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionTitle(section.title, Modifier.padding(top = Spacing.s))
        AppCard(Modifier.fillMaxWidth()) { BlockColumn { section.blocks.forEach { Block(it) } } }
    }
}

@Composable
private fun BlockColumn(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) { content() }
}

@Composable
private fun Block(block: HelpBlock) {
    when (block) {
        is HelpBlock.Paragraph -> Body(block.text)
        is HelpBlock.Subheading -> Text(
            block.text.annotated(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        is HelpBlock.Bullets, is HelpBlock.Steps -> ListItems(block)
    }
}

/** A bullet before each item of a list, its number before each step. */
@Composable
private fun ListItems(block: HelpBlock) {
    val items = when (block) {
        is HelpBlock.Bullets -> block.items
        is HelpBlock.Steps -> block.items
        else -> return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        items.forEachIndexed { index, item ->
            val marker = if (block is HelpBlock.Steps) {
                stringResource(Res.string.help_step_number, index + 1)
            } else {
                stringResource(Res.string.help_bullet)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(
                    marker,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.widthIn(min = Sizes.iconS),
                )
                Body(item, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Body(text: HelpText, modifier: Modifier = Modifier) {
    Text(
        text.annotated(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

private fun HelpText.annotated(): AnnotatedString = buildAnnotatedString {
    spans.forEach { span ->
        if (span.bold) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(span.text) } else append(span.text)
    }
}

@Preview
@Composable
private fun HelpScreenPreview() = AppTheme {
    fun text(vararg spans: HelpSpan) = HelpText(spans.toList())
    val page = HelpPage(
        HelpAudience.LEADERS,
        intro = listOf(
            HelpBlock.Paragraph(text(HelpSpan("Cette page s'adresse aux "), HelpSpan("responsables", bold = true), HelpSpan("."))),
        ),
        sections = listOf(
            HelpSection(
                "Publier la setlist de la semaine",
                listOf(
                    HelpBlock.Steps(
                        listOf(text(HelpSpan("Dans Semaine, touche « Publier la setlist ».")), text(HelpSpan("Range les chants."))),
                    ),
                    HelpBlock.Subheading(text(HelpSpan("Le mode scène"))),
                    HelpBlock.Bullets(listOf(text(HelpSpan("A+ et A− changent la taille du texte.")))),
                ),
            ),
        ),
    )
    HelpScreenContent(
        state = deriveHelpState(listOf(page, page.copy(audience = HelpAudience.MEMBERS)), chosen = null),
        onEvent = {},
        onBack = {},
    )
}
