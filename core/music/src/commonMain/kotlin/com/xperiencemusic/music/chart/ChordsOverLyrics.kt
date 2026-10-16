package com.xperiencemusic.music.chart

import com.xperiencemusic.music.theory.Chord
import com.xperiencemusic.music.theory.Key

/**
 * Turns a chart written the musicians' way, chords on their own line above the lyrics, into the
 * app's inline format: each chord goes into the lyric line below, at the column it stood over.
 *
 * ```
 * Do        Sol          [Do]Tu es saint, [Sol]Seigneur
 * Tu es saint, Seigneur  →
 * ```
 *
 * A chord line with no lyrics under it stays a line of chords, and headings such as "Refrain :" or
 * "Couplet 1" become sections. Lines already in the inline format are left as they are.
 */
object ChordsOverLyrics {
    private val sectionWords = listOf(
        "refrain", "couplet", "strophe", "pont", "pré-refrain", "pre-refrain", "intro", "outro",
        "final", "fin", "coda", "interlude", "bridge", "chorus", "verse", "pre-chorus", "tag",
    )
    private val sectionHeading = Regex("""^\[?\(?\s*([A-Za-zÀ-ÿ-]+)(\s*\d+)?\s*\)?\]?\s*:?\s*$""")
    private val barMarks = setOf("|", "||", "/", "-", "%", "(", ")")

    /** True when [text] has at least one chord line sitting over a lyric line. */
    fun applies(text: String, key: Key): Boolean {
        val lines = text.lines()
        return lines.indices.any { i -> isChordLine(lines[i], key) && lines.getOrNull(i + 1)?.let { isLyricLine(it, key) } == true }
    }

    fun toInline(text: String, key: Key): String {
        val lines = text.replace("\t", "    ").lines()
        val out = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val next = lines.getOrNull(i + 1)
            when {
                isChordLine(line, key) && next != null && isLyricLine(next, key) -> {
                    out += merge(line, next)
                    i += 2
                    continue
                }
                isChordLine(line, key) -> out += chordsOnly(line)
                else -> out += sectionTitle(line)?.let { "# $it" } ?: line.trimEnd()
            }
            i++
        }
        return out.joinToString("\n")
    }

    /** "Refrain :", "[Couplet 2]", "PONT" → the section name; null for any other line. */
    private fun sectionTitle(line: String): String? {
        val match = sectionHeading.matchEntire(line.trim()) ?: return null
        val word = match.groupValues[1]
        if (word.lowercase() !in sectionWords) return null
        val name = word.lowercase().replaceFirstChar { it.uppercase() }
        return name + match.groupValues[2].trimEnd()
    }

    private fun isChordLine(line: String, key: Key): Boolean {
        if (line.contains('[')) return false
        val tokens = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() && it !in barMarks }
        return tokens.isNotEmpty() && tokens.all { looksLikeChord(it, key) }
    }

    /** Chords are written capitalised ("Do", "Am", "6m"): "la la la" stays lyrics. */
    private fun looksLikeChord(token: String, key: Key): Boolean {
        val first = token.first()
        val capitalised = first.isUpperCase() || first.isDigit() || first == 'b' && token.length > 1 && token[1].isDigit()
        return capitalised && Chord.parse(token, key) != null
    }

    private fun isLyricLine(line: String, key: Key): Boolean =
        line.isNotBlank() && !isChordLine(line, key) && sectionTitle(line) == null && !line.trimStart().startsWith("#")

    private fun chordsOnly(line: String): String =
        line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() && it !in barMarks }.joinToString(" ") { "[$it]" }

    /** Puts each chord of [chords] into [lyrics] at the column it starts at, rightmost first. */
    private fun merge(chords: String, lyrics: String): String {
        val placed = Regex("""\S+""").findAll(chords)
            .filter { it.value !in barMarks }
            .map { it.range.first to it.value }
            .toList()
        val width = maxOf(lyrics.length, placed.maxOfOrNull { it.first } ?: 0)
        val text = StringBuilder(lyrics.padEnd(width))
        placed.asReversed().forEach { (column, chord) -> text.insert(column, "[$chord]") }
        return text.toString().trimEnd()
    }
}
