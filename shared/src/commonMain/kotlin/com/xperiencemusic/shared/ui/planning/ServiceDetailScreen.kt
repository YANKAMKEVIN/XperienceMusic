package com.xperiencemusic.shared.ui.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.ServiceKind
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.planning.PlanningError
import com.xperiencemusic.shared.presentation.planning.RosterRowUi
import com.xperiencemusic.shared.presentation.planning.ServiceDetailEvent
import com.xperiencemusic.shared.presentation.planning.ServiceDetailUiState
import com.xperiencemusic.shared.presentation.planning.ServiceDetailViewModel
import com.xperiencemusic.shared.presentation.planning.ServingUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.error_forbidden
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.planning_nobody
import com.xperiencemusic.shared.resources.service_friday_free
import com.xperiencemusic.shared.resources.service_roster
import com.xperiencemusic.shared.resources.service_roster_hint
import com.xperiencemusic.shared.resources.service_setlist
import com.xperiencemusic.shared.resources.service_setlist_none
import com.xperiencemusic.shared.resources.service_team
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.SectionTitle
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ServiceDetailScreen(
    date: LocalDate,
    onBack: () -> Unit,
    viewModel: ServiceDetailViewModel = koinViewModel(key = "service-$date") { parametersOf(date) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ServiceDetailScreenContent(state, viewModel::onEvent, onBack)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServiceDetailScreenContent(state: ServiceDetailUiState, onEvent: (ServiceDetailEvent) -> Unit, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbar.showSnackbar(getString(if (error == PlanningError.FORBIDDEN) Res.string.error_forbidden else Res.string.error_generic))
            onEvent(ServiceDetailEvent.ErrorShown)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.replaceFirstChar { it.uppercase() }) },
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
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            item {
                AppCard {
                    SectionTitle(stringResource(Res.string.service_team))
                    if (state.serving.isEmpty()) {
                        Text(
                            stringResource(Res.string.planning_nobody),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.s),
                        )
                    } else {
                        Instrument.entries.forEach { instrument ->
                            val people = state.serving.filter { it.instrument == instrument }
                            if (people.isNotEmpty()) {
                                Text(
                                    instrument.label(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Spacing.s),
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                                    modifier = Modifier.padding(top = Spacing.xs),
                                ) { people.forEach { PersonPill(it) } }
                            }
                        }
                    }
                    MissingLine(state.missing, Modifier.padding(top = Spacing.m))
                }
            }
            item {
                AppCard {
                    SectionTitle(stringResource(Res.string.service_setlist))
                    when {
                        state.kind == ServiceKind.FRIDAY -> Note(stringResource(Res.string.service_friday_free))
                        state.songTitles.isEmpty() -> Note(stringResource(Res.string.service_setlist_none))
                        else -> state.songTitles.forEachIndexed { index, title ->
                            Text("${index + 1}  $title", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = Spacing.s))
                        }
                    }
                }
            }
            if (state.roster.isNotEmpty()) {
                item {
                    Column(Modifier.padding(top = Spacing.s)) {
                        SectionTitle(stringResource(Res.string.service_roster))
                        Note(stringResource(Res.string.service_roster_hint))
                    }
                }
                items(state.roster, key = { it.member.memberId }) { row ->
                    RosterRow(row, enabled = state.savingMemberId == null, onEvent)
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.xs))
}

@Composable
private fun RosterRow(row: RosterRowUi, enabled: Boolean, onEvent: (ServiceDetailEvent) -> Unit) {
    val part = row.member.voicePart?.label() ?: row.member.instrument?.label()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = row.serves == true,
            onCheckedChange = { onEvent(ServiceDetailEvent.Toggled(row.member.memberId, it)) },
            enabled = enabled,
        )
        Column(Modifier.weight(1f)) {
            Text(row.member.name, style = MaterialTheme.typography.bodyLarge)
            if (part != null) Text(part, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview
@Composable
private fun ServiceDetailScreenPreview() {
    val kevin = ServingUi("k", "Kevin", Instrument.SINGER, VoicePart.TENOR, true)
    AppTheme {
        ServiceDetailScreenContent(
            ServiceDetailUiState(
                isLoading = false,
                date = LocalDate(2026, 11, 1),
                title = "dimanche 1 nov.",
                serving = listOf(ServingUi("g", "Grâce", Instrument.SINGER, VoicePart.SOPRANO, false), kevin),
                missing = listOf(Instrument.BASS),
                songTitles = listOf("À toi la gloire", "Holy, Holy, Holy"),
                roster = listOf(RosterRowUi(kevin, true), RosterRowUi(ServingUi("p", "Paul", Instrument.DRUMS, null, false), null)),
            ),
            onEvent = {}, onBack = {},
        )
    }
}
