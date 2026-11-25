package com.xperiencemusic.shared.ui.song

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.xperiencemusic.music.chart.ChartLine
import com.xperiencemusic.music.chart.ChartSegment
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.song_chart_empty
import com.xperiencemusic.shared.resources.song_chart_loading
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * Lyrics with each chord printed above the syllable it falls on; lines wrap between words.
 * With [lyricsOnly] the chords go: plain lines of text, twice as many on a screen, for singers.
 */
@OptIn(ExperimentalLayoutApi::class)
/** False for the blank lines an empty chart is made of. */
internal fun ChartLine.hasContent(): Boolean = this is ChartLine.Section || (this is ChartLine.Lyrics && segments.isNotEmpty())

/** [isLoading]: the chart is not on the phone yet, which a line says in place of [lines]. */
@Composable
fun ChordChartView(
    lines: List<ChartLine>,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    lyricsOnly: Boolean = false,
    isLoading: Boolean = false,
) {
    val body = MaterialTheme.typography.bodyLarge.scaled(scale)
    if (isLoading) {
        Text(stringResource(Res.string.song_chart_loading), modifier, style = body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    // A song added with its title and a link only: say so, an empty card read as a failure.
    if (lines.none { it.hasContent() }) {
        Text(stringResource(Res.string.song_chart_empty), modifier, style = body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val section = MaterialTheme.typography.labelLarge.scaled(scale)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs * scale)) {
        lines.forEach { line ->
            when (line) {
                is ChartLine.Section -> Text(
                    line.title.uppercase(),
                    style = section,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = Spacing.m),
                )
                is ChartLine.Lyrics ->
                    if (line.segments.isEmpty()) {
                        Spacer(Modifier.height(Spacing.s))
                    } else if (lyricsOnly) {
                        // A line of chords alone (an intro) has nothing to sing: it is left out.
                        line.lyricsOnly()?.let { Text(it, style = body) }
                    } else {
                        // Wraps between words only: "Ressus-" never ends a line with "cité" on the next.
                        FlowRow {
                            line.words().forEach { word -> Row { word.forEach { ChartSegmentView(it, body) } } }
                        }
                    }
            }
        }
    }
}

/**
 * Chord over lyric. The segment is as wide as the wider of the two, the chord keeping a gap before the
 * next one. Inside a word that carries on in the next segment ("Ressus[5]ci[1]té") that gap is the
 * narrowest that keeps two chords apart, and when the chord still overhangs the syllable a hyphen
 * fills the gap so the word reads as one instead of "Ressusci té".
 */
@Composable
private fun ChartSegmentView(segment: ChartSegment, style: TextStyle) {
    val chordGap = if (segment.continuesWord) Spacing.xs else Spacing.s
    Layout(
        content = {
            Text(
                segment.chord ?: " ",
                style = style,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(segment.lyric.ifEmpty { " " }, style = style)
            Text("-", style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val chord = measurables[0].measure(loose)
        val lyric = measurables[1].measure(loose)
        val hyphen = measurables[2].measure(loose)
        // The gap grows with the text: at stage size the token alone left two chords touching.
        val gapRatio = if (segment.continuesWord) 0.3f else 0.5f
        val gap = maxOf(chordGap.roundToPx(), (style.fontSize * gapRatio).roundToPx())
        val chordWidth = if (segment.chord != null) chord.width + gap else 0
        // Any room left after the syllable would split the word on screen, so a hyphen bridges it.
        val showHyphen = segment.continuesWord && chordWidth > lyric.width
        val width = if (showHyphen) maxOf(chordWidth, lyric.width + hyphen.width) else maxOf(chordWidth, lyric.width)
        layout(width, chord.height + lyric.height) {
            chord.placeRelative(0, 0)
            lyric.placeRelative(0, chord.height)
            if (showHyphen) {
                val gapCenter = lyric.width + (width - lyric.width - hyphen.width) / 2
                hyphen.placeRelative(gapCenter, chord.height)
            }
        }
    }
}

private fun TextStyle.scaled(scale: Float) =
    if (scale == 1f) this else copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)
