package com.xperiencemusic.music.chart

import com.xperiencemusic.music.theory.Chord
import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Notation

/**
 * Lyrics with inline chords, ChordPro style: `[1]Tu es [5]saint, [6m]Sei[4]gneur`.
 * A line starting with `#` is a section title (`# Refrain`).
 *
 * Chords may be typed in any notation; [normalize] stores them as Nashville numbers so the chart can
 * be rendered in any key and notation later.
 */
object ChordChart {
    private val chordToken = Regex("""\[([^\]]*)\]""")

    /** Rewrites every chord of [source] as Nashville numbers relative to [key]. Unknown tokens are kept. */
    fun normalize(source: String, key: Key): String = convert(source, key, Notation.NASHVILLE)

    /** Rewrites every chord of [source], read in [key], in [notation]. Unknown tokens are kept. */
    fun convert(source: String, key: Key, notation: Notation): String =
        chordToken.replace(source) { match ->
            val chord = Chord.parse(match.groupValues[1], key)
            if (chord == null) match.value else "[${chord.render(key, notation)}]"
        }

    /** Chord tokens of [source] that are not chords, e.g. typos; "N.C." and repeat marks show up too. */
    fun unknownChords(source: String, key: Key): List<String> =
        chordToken.findAll(source).map { it.groupValues[1] }.filter { Chord.parse(it, key) == null }.toList()

    fun render(source: String, key: Key, notation: Notation): List<ChartLine> =
        source.lines().map { line -> renderLine(line, key, notation) }

    private fun renderLine(line: String, key: Key, notation: Notation): ChartLine {
        if (line.trimStart().startsWith("#")) {
            return ChartLine.Section(line.trimStart().removePrefix("#").trim())
        }
        val segments = mutableListOf<ChartSegment>()
        var cursor = 0
        var pendingChord: String? = null
        chordToken.findAll(line).forEach { match ->
            val lyric = line.substring(cursor, match.range.first)
            if (pendingChord != null || lyric.isNotEmpty()) segments += ChartSegment(pendingChord, lyric)
            val token = match.groupValues[1]
            pendingChord = Chord.parse(token, key)?.render(key, notation) ?: token
            cursor = match.range.last + 1
        }
        val tail = line.substring(cursor)
        if (pendingChord != null || tail.isNotEmpty()) segments += ChartSegment(pendingChord, tail)
        return ChartLine.Lyrics(markSplitWords(segments))
    }

    /** Flags fragments whose word goes on in the next fragment ("Ressus[5]cité"). */
    private fun markSplitWords(segments: List<ChartSegment>): List<ChartSegment> =
        segments.mapIndexed { index, segment ->
            val next = segments.getOrNull(index + 1)?.lyric
            val continuesWord = segment.lyric.lastOrNull()?.isLetter() == true && next?.firstOrNull()?.isLetter() == true
            if (continuesWord) segment.copy(continuesWord = true) else segment
        }
}

sealed interface ChartLine {
    data class Section(val title: String) : ChartLine
    data class Lyrics(val segments: List<ChartSegment>) : ChartLine {
        /**
         * The line cut into words that must stay together on screen, so it only wraps between two
         * words and never inside "Ressus[5]cité". Each word keeps its chords and the space after it.
         * Punctuation set apart by a space ("cité !", "« Gloire") stays with its word.
         */
        fun words(): List<List<ChartSegment>> {
            val words = mutableListOf<MutableList<ChartSegment>>()
            var previous: ChartSegment? = null
            pieces().forEach { piece ->
                val glued = previous?.let { before ->
                    !before.lyric.endsWithSpace() ||
                        piece.lyric.isPunctuation(CLOSING) ||
                        before.lyric.isPunctuation(OPENING)
                } ?: false
                if (glued) words.last() += piece else words += mutableListOf(piece)
                previous = piece
            }
            return words
        }

        /** One piece per word: the chord goes with the first word of its fragment. */
        private fun pieces(): List<ChartSegment> = segments.flatMap { segment ->
            val tokens = WORD.findAll(segment.lyric).map { it.value }.filter { it.isNotEmpty() }.toList().ifEmpty { listOf("") }
            tokens.mapIndexed { index, token ->
                ChartSegment(
                    chord = if (index == 0) segment.chord else null,
                    lyric = token,
                    continuesWord = index == tokens.lastIndex && segment.continuesWord,
                )
            }
        }

        private companion object {
            val WORD = Regex("""\S*\s*""")
            const val CLOSING = "!?:;»…"
            const val OPENING = "«"

            fun String.endsWithSpace() = lastOrNull()?.isWhitespace() == true
            fun String.isPunctuation(marks: String) = trim().let { it.isNotEmpty() && it.all { char -> char in marks } }
        }
    }
}

/**
 * A chord sitting above the lyric fragment that follows it. Either part may be empty.
 * [continuesWord] is true when the fragment stops mid-word, so a display can bridge the gap left by
 * a chord wider than the syllable.
 */
data class ChartSegment(val chord: String?, val lyric: String, val continuesWord: Boolean = false)
