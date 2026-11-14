package com.xperiencemusic.shared.ui.week

import com.xperiencemusic.shared.resources.action_upload_rehearsal
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.semantics.contentDescription
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
import com.xperiencemusic.shared.resources.week_countdown_caption
import com.xperiencemusic.shared.resources.week_countdown_caption_today
import com.xperiencemusic.shared.resources.week_countdown_days
import com.xperiencemusic.shared.resources.week_countdown_text
import com.xperiencemusic.shared.resources.week_countdown_title
import com.xperiencemusic.shared.resources.week_countdown_today
import com.xperiencemusic.shared.resources.week_empty_text
import com.xperiencemusic.shared.resources.week_empty_title
import com.xperiencemusic.shared.resources.week_past_hint
import com.xperiencemusic.shared.resources.week_pause_reference
import com.xperiencemusic.shared.resources.week_play_reference
import com.xperiencemusic.shared.resources.week_prepare_next
import com.xperiencemusic.shared.resources.week_published_by
import com.xperiencemusic.shared.resources.week_setlist_count
import com.xperiencemusic.shared.resources.week_this_week
import com.xperiencemusic.shared.resources.week_title
import com.xperiencemusic.shared.resources.account_open
import com.xperiencemusic.shared.presentation.common.MemberAvatarUi
import com.xperiencemusic.shared.ui.components.MemberAvatar
import com.xperiencemusic.shared.ui.brand.BrandWaves
import com.xperiencemusic.shared.ui.brand.KeyPill
import com.xperiencemusic.shared.ui.brand.TabHeader
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.CountdownEmptyState
import com.xperiencemusic.shared.ui.components.EmptyState
import com.xperiencemusic.shared.ui.components.OfflineBanner
import com.xperiencemusic.shared.ui.components.PauseIcon
import com.xperiencemusic.shared.ui.components.choiceState
import com.xperiencemusic.shared.ui.format.rememberFormattingWords
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.xperiencemusic.shared.ui.components.LoadingIndicator

private val WAVES_HEIGHT = 44.dp
/** The column of a song's number: two digits wide. */
private val POSITION_WIDTH = 28.dp
private val PLAY_BUTTON_SIZE = 40.dp

/** [onEditWeek] opens the editor on a week's service date, or on the next free week when null. */
@Composable
fun WeekScreen(
    onSongClick: (String) -> Unit,
    onEditWeek: (LocalDate?) -> Unit,
    onUploadRehearsal: (LocalDate) -> Unit,
    onAccount: () -> Unit,
    viewModel: WeekViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WeekScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onSongClick = onSongClick,
        onEditWeek = onEditWeek,
        onUploadRehearsal = onUploadRehearsal,
        onAccount = onAccount,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreenContent(
    state: WeekUiState,
    onEvent: (WeekEvent) -> Unit,
    onSongClick: (String) -> Unit,
    onEditWeek: (LocalDate?) -> Unit,
    onUploadRehearsal: (LocalDate) -> Unit = {},
    onAccount: () -> Unit = {},
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(WeekEvent.RefreshPulled) },
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.isLoading) {
            LoadingIndicator()
        } else {
            WeekList(state, onEvent, onSongClick, onEditWeek, onUploadRehearsal, onAccount)
        }
    }
}

@Composable
private fun WeekList(
    state: WeekUiState,
    onEvent: (WeekEvent) -> Unit,
    onSongClick: (String) -> Unit,
    onEditWeek: (LocalDate?) -> Unit,
    onUploadRehearsal: (LocalDate) -> Unit = {},
    onAccount: () -> Unit,
) {
    val setlist = state.setlist
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.l, top = Spacing.l, end = Spacing.l, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { WeekHeader(setlist?.let { rememberFormattingWords().range(it.rehearsalDate, it.serviceDate) }, state.me, onAccount) }
        if (state.weeks.size > 1) item { WeekTabs(state.weeks, onSelect = { onEvent(WeekEvent.WeekSelected(it)) }) }
        if (state.isOffline) item { OfflineBanner() }
        if (setlist == null) {
            item { EmptyWeek(state, onPublish = { onEditWeek(null) }) }
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
                item { BrandWaves(Modifier.fillMaxWidth().height(WAVES_HEIGHT)) }
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
        // With no setlist, the empty state carries the publish button.
        if (state.canPublish && setlist != null) {
            item { AdminActions(state, onEditWeek, onUploadRehearsal) }
        }
    }
}

/**
 * Admins are invited to publish; members, who cannot, see how long until the rehearsal instead.
 */
@Composable
private fun EmptyWeek(state: WeekUiState, onPublish: () -> Unit) {
    val countdown = state.nextRehearsal
    if (state.canPublish || countdown == null) {
        EmptyState(
            icon = XmIcons.Notes,
            title = stringResource(Res.string.week_empty_title),
            text = stringResource(Res.string.week_empty_text),
            actionLabel = if (state.canPublish) stringResource(Res.string.action_publish_setlist) else null,
            onAction = onPublish,
        )
    } else {
        CountdownEmptyState(
            value = if (countdown.daysLeft == 0) stringResource(Res.string.week_countdown_today) else stringResource(Res.string.week_countdown_days, countdown.daysLeft),
            caption = stringResource(if (countdown.daysLeft == 0) Res.string.week_countdown_caption_today else Res.string.week_countdown_caption),
            fraction = countdown.fraction,
            title = stringResource(Res.string.week_countdown_title, rememberFormattingWords().dayAndDate(countdown.date)),
            text = stringResource(Res.string.week_countdown_text),
        )
    }
}

@Composable
private fun WeekHeader(rangeLabel: String?, me: MemberAvatarUi?, onAccount: () -> Unit) {
    TabHeader(stringResource(Res.string.week_title), subtitle = rangeLabel) {
        if (me != null) {
            val label = stringResource(Res.string.account_open, me.name)
            // 48 dp to touch, 40 dp to see, like an icon button.
            Box(
                Modifier.size(Sizes.touchTarget).clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onAccount)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                MemberAvatar(me, size = Sizes.avatarS)
            }
        }
    }
}

/** The kept weeks, opened on the selected one: past weeks stay readable, coming ones are ready. */
@Composable
private fun WeekTabs(weeks: List<WeekTabUi>, onSelect: (LocalDate) -> Unit) {
    val words = rememberFormattingWords()
    val listState = rememberLazyListState()
    val selected = weeks.indexOfFirst { it.isSelected }
    LaunchedEffect(selected) { if (selected > 0) listState.animateScrollToItem((selected - 1).coerceAtLeast(0)) }
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        items(weeks, key = { it.serviceDate.toString() }) { week ->
            FilterChip(
                selected = week.isSelected,
                onClick = { onSelect(week.serviceDate) },
                label = {
                    Text(if (week.timing == WeekTiming.CURRENT) stringResource(Res.string.week_this_week) else words.shortDayAndDate(week.serviceDate))
                },
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = if (week.timing == WeekTiming.PAST) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                modifier = Modifier.choiceState(week.isSelected),
            )
        }
    }
}

@Composable
private fun DatesRow(setlist: WeekSetlistUi) {
    val words = rememberFormattingWords()
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), modifier = Modifier.height(IntrinsicSize.Min)) {
        DateCard(stringResource(Res.string.setlist_rehearsal_label), words.dayAndDate(setlist.rehearsalDate), Modifier.weight(1f).fillMaxHeight())
        DateCard(stringResource(Res.string.setlist_service_label), words.dayAndDate(setlist.serviceDate), Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun DateCard(label: String, date: String, modifier: Modifier = Modifier) {
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
                modifier = Modifier.width(POSITION_WIDTH),
            )
            // A past week reads as history: the title fades with its key, not the key alone.
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (dimmed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            KeyPill(song.keyLabel, dimmed = dimmed, modifier = Modifier.padding(horizontal = Spacing.s))
            if (song.hasReference) {
                // The label sits on the button itself, so a screen reader hears "pause" as soon as the icon shows it.
                val label = if (song.isPlaying) stringResource(Res.string.week_pause_reference, song.title)
                else stringResource(Res.string.week_play_reference, song.title)
                FilledTonalIconButton(onClick = onPlay, modifier = Modifier.size(PLAY_BUTTON_SIZE).semantics { contentDescription = label }) {
                    Icon(if (song.isPlaying) PauseIcon else Icons.Filled.PlayArrow, contentDescription = null)
                }
            } else {
                // Keeps titles and keys aligned with the rows that have a play button.
                Box(Modifier.size(PLAY_BUTTON_SIZE))
            }
        }
    }
}

@Composable
private fun AdminActions(state: WeekUiState, onEditWeek: (LocalDate?) -> Unit, onUploadRehearsal: (LocalDate) -> Unit) {
    val setlist = state.setlist
    Column(Modifier.padding(top = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (setlist != null) {
            OutlinedButton(
                onClick = { onUploadRehearsal(setlist.serviceDate) },
                border = BorderStroke(Sizes.hairline, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().height(Sizes.actionHeight),
            ) {
                Icon(XmIcons.Mic, contentDescription = null, modifier = Modifier.size(Sizes.iconS))
                Text(stringResource(Res.string.action_upload_rehearsal), modifier = Modifier.padding(start = Spacing.s))
            }
        }
        if (state.canEditSelected && setlist != null) {
            OutlinedButton(
                onClick = { onEditWeek(setlist.serviceDate) },
                border = BorderStroke(Sizes.hairline, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().height(Sizes.actionHeight),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(Sizes.iconS))
                Text(stringResource(Res.string.action_edit_setlist), modifier = Modifier.padding(start = Spacing.s))
            }
        }
        TextButton(onClick = { onEditWeek(null) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(Sizes.iconS))
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
                    WeekTabUi(LocalDate(2026, 10, 11), WeekTiming.PAST, isSelected = false),
                    WeekTabUi(LocalDate(2026, 10, 18), WeekTiming.CURRENT, isSelected = true),
                    WeekTabUi(LocalDate(2026, 10, 25), WeekTiming.AHEAD, isSelected = false),
                ),
                setlist = WeekSetlistUi(
                    serviceDate = LocalDate(2026, 10, 18),
                    timing = WeekTiming.CURRENT,
                    rehearsalDate = LocalDate(2026, 10, 13),
                    message = "On reprend l’intro acoustique.",
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
