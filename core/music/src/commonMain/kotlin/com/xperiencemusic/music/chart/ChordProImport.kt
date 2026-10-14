package com.xperiencemusic.music.chart

import com.xperiencemusic.music.theory.Key

/**
 * A song read from a ChordPro file. [chart] is in the app's chart format (see [ChordChart]) with the
 * chords as written in the file; [ChordChart.normalize] turns them into Nashville numbers on save.
 */
data class ImportedChart(
    val title: String?,
    val artist: String?,
    val key: Key?,
    val chart: String,
    /** Comments that are not section titles: copyright, publisher, collection number. */
    val credits: List<String>,
)

/**
 * Reads standard ChordPro: `{t:}`, `{st:}` and `{key:}` fill the song, `[Am]` chords stay inline,
 * and `{start_of_chorus}` style blocks become `# Refrain` sections. A `{c:}` comment right before a
 * block or a lyric line is that section's title; the other comments are kept as credits.
 */
object ChordProImport {
    private val directive = Regex("""^\{\s*([A-Za-z_]+)\s*(?::\s*(.*?))?\s*}$""")
    private val commentNames = setOf("c", "comment", "ci", "comment_italic", "cb", "comment_box")

    fun parse(source: String): ImportedChart {
        val lines = source.lines().map { it.trimEnd() }
        var title: String? = null
        var artist: String? = null
        var key: Key? = null
        val credits = mutableListOf<String>()
        val chart = mutableListOf<String>()
        var sectionJustOpened = false

        lines.forEachIndexed { index, line ->
            if (line.isBlank()) return@forEachIndexed
            val match = directive.matchEntire(line.trim())
            if (match == null) {
                chart += line
                sectionJustOpened = false
                return@forEachIndexed
            }
            val name = match.groupValues[1].lowercase()
            val value = match.groupValues[2].trim()
            when {
                name == "t" || name == "title" -> title = value.ifEmpty { null }
                name == "st" || name == "subtitle" || name == "artist" -> artist = value.ifEmpty { null }
                name == "key" -> key = Key.parse(value)
                name in commentNames && value.isNotEmpty() ->
                    if (opensSection(lines, index)) {
                        chart += "# $value"
                        sectionJustOpened = true
                    } else {
                        credits += value
                    }
                name == "soc" || name == "start_of_chorus" -> if (!sectionJustOpened) chart += "# Refrain"
                name == "sob" || name == "start_of_bridge" -> if (!sectionJustOpened) chart += "# Pont"
            }
        }
        return ImportedChart(title, artist, key, chart.joinToString("\n"), credits)
    }

    /** True when the next meaningful line after [index] is a block start or a lyric line. */
    private fun opensSection(lines: List<String>, index: Int): Boolean {
        val next = lines.drop(index + 1).firstOrNull { it.isNotBlank() }?.trim() ?: return false
        val name = directive.matchEntire(next)?.groupValues?.get(1)?.lowercase() ?: return true
        return name.startsWith("start_of_") || name in setOf("soc", "sov", "sob")
    }
}
