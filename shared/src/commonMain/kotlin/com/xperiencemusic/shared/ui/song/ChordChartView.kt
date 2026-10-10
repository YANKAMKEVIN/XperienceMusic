package com.xperiencemusic.shared.ui.song

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import com.xperiencemusic.music.chart.ChartLine
import com.xperiencemusic.music.chart.ChartSegment
import com.xperiencemusic.shared.ui.theme.Spacing

/** Lyrics with each chord printed above the syllable it falls on. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChordChartView(lines: List<ChartLine>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        lines.forEach { line ->
            when (line) {
                is ChartLine.Section -> Text(
                    line.title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = Spacing.m),
                )
                is ChartLine.Lyrics ->
                    if (line.segments.isEmpty()) {
                        Spacer(Modifier.height(Spacing.s))
                    } else {
                        FlowRow { line.segments.forEach { ChartSegmentView(it) } }
                    }
            }
        }
    }
}

/**
 * Chord over lyric. The segment is as wide as the wider of the two; when the chord is wider and the
 * word carries on in the next segment, a hyphen fills the gap so the word still reads as one.
 */
@Composable
private fun ChartSegmentView(segment: ChartSegment) {
    val chordGap = Spacing.s
    Layout(
        content = {
            Text(
                segment.chord ?: " ",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(segment.lyric.ifEmpty { " " }, style = MaterialTheme.typography.bodyLarge)
            Text("-", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val chord = measurables[0].measure(loose)
        val lyric = measurables[1].measure(loose)
        val hyphen = measurables[2].measure(loose)
        val chordWidth = if (segment.chord != null) chord.width + chordGap.roundToPx() else 0
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
