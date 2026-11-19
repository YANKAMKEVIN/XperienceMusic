package com.xperiencemusic.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xperiencemusic.shared.presentation.common.lyricExcerpt
import com.xperiencemusic.shared.presentation.common.searchMatches
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.presentation.library.LibrarySongUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.library_has_audio
import com.xperiencemusic.shared.resources.library_sung_on
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.format.rememberFormattingWords
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * A song as a compact line, without a card: title, then the lyric line a search matched or "Chanté
 * le …" and the author, a note when it has audio, and [trailing] at the end (the "+" of the setlist
 * editor). The same line in the library and wherever songs are picked.
 */
@Composable
internal fun SongListRow(
    song: LibrarySongUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    startPadding: Dp = Spacing.l,
    endPadding: Dp = Spacing.l,
    /** The search that found this song: its letters stand out in the title and in the line below. */
    highlight: String = "",
    trailing: (@Composable () -> Unit)? = null,
) {
    val words = rememberFormattingWords()
    val subtitle = song.lyricLine?.let { lyricExcerpt(it, highlight) }
        ?: listOfNotNull(
            song.lastSungOn?.let { stringResource(Res.string.library_sung_on, words.dayAndMonth(it)) },
            song.artist,
        ).joinToString(" · ").ifEmpty { null }
    Column(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.heightIn(min = ROW_MIN_HEIGHT).padding(start = startPadding, end = endPadding, top = Spacing.s, bottom = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                // Among some 2000 songs, what was typed shows at a glance why a result is there:
                // bold in the title, violet in the lyric line or the author.
                Text(
                    highlighted(song.title, highlight, SpanStyle(fontWeight = FontWeight.Bold)),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        highlighted(
                            subtitle,
                            highlight,
                            SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (song.hasAudio) {
                Icon(
                    XmIcons.Notes,
                    contentDescription = stringResource(Res.string.library_has_audio),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = Spacing.m).size(Sizes.iconS),
                )
            }
            trailing?.invoke()
        }
        HorizontalDivider(
            Modifier.padding(start = startPadding, end = endPadding),
            thickness = Sizes.hairline,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** [text] with [style] on the letters [query] matched; the plain text when there is nothing to show. */
@Composable
private fun highlighted(text: String, query: String, style: SpanStyle): AnnotatedString =
    remember(text, query, style) {
        buildAnnotatedString {
            append(text)
            searchMatches(text, query).forEach { addStyle(style, it.first, it.last + 1) }
        }
    }

private val ROW_MIN_HEIGHT = 56.dp
