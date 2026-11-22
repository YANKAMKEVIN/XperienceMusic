package com.xperiencemusic.shared.presentation.common

import com.xperiencemusic.shared.domain.usecase.foldForSearch

/**
 * Where [query] shows in [text], read as the search reads them (case and accents ignored), so a
 * result can show why it was found. Empty when [query] is blank or not there.
 */
fun searchMatches(text: String, query: String): List<IntRange> {
    val needle = query.trim().foldForSearch()
    if (needle.isEmpty()) return emptyList()
    // Folding swaps one character for one: positions in the folded text are positions in the text.
    val haystack = text.foldForSearch()
    val matches = mutableListOf<IntRange>()
    var from = haystack.indexOf(needle)
    while (from >= 0) {
        matches += from until from + needle.length
        from = haystack.indexOf(needle, from + needle.length)
    }
    return matches
}

/**
 * A lyric line shown on one line of a list: when the match sits too far along to be seen, the line
 * starts a few words before it, behind an ellipsis.
 */
fun lyricExcerpt(line: String, query: String, lead: Int = EXCERPT_LEAD): String {
    val start = searchMatches(line, query).firstOrNull()?.first?.takeIf { it > lead } ?: return line
    // Back to the start of a word, so the excerpt does not open in the middle of one.
    val from = line.lastIndexOf(' ', start - lead).takeIf { it >= 0 }?.plus(1)
    return if (from == null || from == 0) line else "…" + line.substring(from)
}

/** About what fits before the match on a phone, at the size of a row's second line. */
private const val EXCERPT_LEAD = 24
