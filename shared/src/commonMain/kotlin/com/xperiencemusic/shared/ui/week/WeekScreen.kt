package com.xperiencemusic.shared.ui.week

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.week.WeekEvent
import com.xperiencemusic.shared.presentation.week.WeekOfflineUi
import com.xperiencemusic.shared.presentation.week.WeekSetlistUi
import com.xperiencemusic.shared.presentation.week.WeekSongUi
import com.xperiencemusic.shared.presentation.week.WeekTabUi
import com.xperiencemusic.shared.presentation.week.WeekTiming
import com.xperiencemusic.shared.presentation.week.WeekUiState
import com.xperiencemusic.shared.presentation.week.WeekViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_edit_setlist
import com.xperiencemusic.shared.resources.action_publish_setlist
import com.xperiencemusic.shared.resources.setlist_rehearsal_label
import com.xperiencemusic.shared.resources.setlist_service_label
import com.xperiencemusic.shared.resources.week_empty
import com.xperiencemusic.shared.resources.week_past_hint
import com.xperiencemusic.shared.resources.week_pause_reference
import com.xperiencemusic.shared.resources.week_play_reference
import com.xperiencemusic.shared.resources.week_prepare_next
import com.xperiencemusic.shared.resources.week_published_by
import com.xperiencemusic.shared.resources.week_setlist_count
import com.xperiencemusic.shared.resources.week_this_week
import com.xperiencemusic.shared.resources.week_title
import com.xperiencemusic.shared.ui.brand.BrandWaves
import com.xperiencemusic.shared.ui.brand.KeyPill
import com.xperiencemusic.shared.ui.brand.XmMonogram
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.OfflineBanner
import com.xperiencemusic.shared.ui.components.PauseIcon
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** [onEditWeek] opens the editor on a week's service date, or on the next free week when null. */
@Composable
fun WeekScreen(
    onSongClick: (String) -> Unit,
    onEditWeek: (LocalDate?) -> Unit,
    viewModel: WeekViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WeekScreenContent(state = state, onEvent = viewModel::onEvent, onSongClick = onSongClick, onEditWeek = onEditWeek)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreenContent(
    state: WeekUiState,
    onEvent: (WeekEvent) -> Unit,
    onSongClick: (String) -> Unit,
    onEditWeek: (LocalDate?) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(WeekEvent.RefreshPulled) },
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            WeekList(state, onEvent, onSongClick, onEditWeek)
        }
    }
}

@Composable
private fun WeekList(
    state: WeekUiState,
    onEvent: (WeekEvent) -> Unit,
    onSongClick: (String) -> Unit,
    onEditWeek: (LocalDate?) -> Unit,
) {
    val setlist = state.setlist
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.l, top = Spacing.l, end = Spacing.l, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { WeekHeader(setlist?.rangeLabel) }
        if (state.weeks.size > 1) item { WeekTabs(state.weeks, onSelect = { onEvent(WeekEvent.WeekSelected(it)) }) }
        if (state.isOffline) item { OfflineBanner() }
        if (setlist == null) {
            item { Text(stringResource(Res.string.week_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item { DatesRow(setlist) }
            if (setlist.timing == WeekTiming.PAST) {
                item {
                    Text(
                        stringResource(Res.string.week_past_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item { BrandWaves(Modifier.fillMaxWidth().height(44.dp)) }
            }
            item {
                Text(
                    stringResource(Res.string.week_setlist_count, setlist.songs.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
                )
            }
            items(setlist.songs, key = { it.id }) { song ->
                WeekSongRow(
                    song,
                    dimmed = setlist.timing == WeekTiming.PAST,
                    onClick = { onSongClick(song.id) },
                    onPlay = { onEvent(WeekEvent.PlayReferenceClicked(song.id)) },
                )
            }
            setlist.message?.let { message -> item { MessageCard(message, setlist.publishedBy) } }
            if (setlist.timing != WeekTiming.PAST) {
                state.offline?.let { item { OfflineCard(it, onDownload = { onEvent(WeekEvent.DownloadClicked) }) } }
            }
        }
        if (state.canPublish) {
            item { AdminActions(state, onEditWeek) }
        }
    }
}

@Composable
private fun WeekHeader(rangeLabel: String?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        XmMonogram(size = 56.dp)
        Text(
            stringResource(Res.string.week_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
        )
        rangeLabel?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The kept weeks, opened on the selected one: past weeks stay readable, coming ones are ready. */
@Composable
private fun WeekTabs(weeks: List<WeekTabUi>, onSelect: (LocalDate) -> Unit) {
    val listState = rememberLazyListState()
    val selected = weeks.indexOfFirst { it.isSelected }
    LaunchedEffect(selected) { if (selected > 0) listState.animateScrollToItem((selected - 1).coerceAtLeast(0)) }
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        items(weeks, key = { it.serviceDate.toString() }) { week ->
            FilterChip(
                selected = week.isSelected,
                onClick = { onSelect(week.serviceDate) },
                label = {
                    Text(if (week.timing == WeekTiming.CURRENT) stringResource(Res.string.week_this_week) else week.label)
                },
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = if (week.timing == WeekTiming.PAST) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun DatesRow(setlist: WeekSetlistUi) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        DateCard(stringResource(Res.string.setlist_rehearsal_label), setlist.rehearsalDate, Modifier.weight(1f))
        DateCard(stringResource(Res.string.setlist_service_label), setlist.serviceDateLabel, Modifier.weight(1f))
    }
}

@Composable
private fun DateCard(label: String, date: String, modifier: Modifier) {
    AppCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = Spacing.m)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(
                    date.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MessageCard(message: String, publishedBy: String) {
    AppCard {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(Res.string.week_published_by, publishedBy),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.s),
        )
    }
}

@Composable
private fun WeekSongRow(song: WeekSongUi, dimmed: Boolean, onClick: () -> Unit, onPlay: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${song.position}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp),
            )
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            KeyPill(song.keyLabel, dimmed = dimmed, modifier = Modifier.padding(horizontal = Spacing.s))
            if (song.hasReference) {
                FilledTonalIconButton(onClick = onPlay, modifier = Modifier.size(40.dp)) {
                    if (song.isPlaying) {
                        Icon(PauseIcon, contentDescription = stringResource(Res.string.week_pause_reference, song.title))
                    } else {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(Res.string.week_play_reference, song.title))
                    }
                }
            } else {
                // Keeps titles and keys aligned with the rows that have a play button.
                Box(Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun AdminActions(state: WeekUiState, onEditWeek: (LocalDate?) -> Unit) {
    val setlist = state.setlist
    Column(Modifier.padding(top = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (state.canEditSelected && setlist != null) {
            OutlinedButton(
                onClick = { onEditWeek(setlist.serviceDate) },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(Res.string.action_edit_setlist), modifier = Modifier.padding(start = Spacing.s))
            }
        }
        TextButton(onClick = { onEditWeek(null) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                stringResource(if (setlist == null) Res.string.action_publish_setlist else Res.string.week_prepare_next),
                modifier = Modifier.padding(start = Spacing.s),
            )
        }
    }
}

@Preview
@Composable
private fun WeekScreenPreview() {
    AppTheme {
        WeekScreenContent(
            WeekUiState(
                isLoading = false,
                canPublish = true,
                weeks = listOf(
                    WeekTabUi(LocalDate(2026, 10, 11), "dim. 11 oct.", WeekTiming.PAST, isSelected = false),
                    WeekTabUi(LocalDate(2026, 10, 18), "dim. 18 oct.", WeekTiming.CURRENT, isSelected = true),
                    WeekTabUi(LocalDate(2026, 10, 25), "dim. 25 oct.", WeekTiming.AHEAD, isSelected = false),
                ),
                setlist = WeekSetlistUi(
                    serviceDate = LocalDate(2026, 10, 18),
                    timing = WeekTiming.CURRENT,
                    rangeLabel = "13 – 18 oct.",
                    rehearsalDate = "mardi 13 oct.",
                    serviceDateLabel = "dimanche 18 oct.",
                    message = "On reprend l'intro acoustique.",
                    publishedBy = "Responsable musical",
                    songs = listOf(
                        WeekSongUi("1", 1, "Nous élevons nos voix", "Ré", hasReference = true, isPlaying = true),
                        WeekSongUi("2", 2, "Ta lumière", "Sol", hasReference = true),
                        WeekSongUi("3", 3, "Fidèle à jamais", "Do", hasReference = false),
                    ),
                ),
                offline = WeekOfflineUi(available = 2, total = 6, isComplete = false, isDownloading = false, hasError = false),
            ),
            onEvent = {},
            onSongClick = {},
            onEditWeek = {},
        )
    }
}

@Preview
@Composable
private fun WeekScreenEmptyPreview() {
    AppTheme { WeekScreenContent(WeekUiState(isLoading = false, canPublish = true), onEvent = {}, onSongClick = {}, onEditWeek = {}) }
}
