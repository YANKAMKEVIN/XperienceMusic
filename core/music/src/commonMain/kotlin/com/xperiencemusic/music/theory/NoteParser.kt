package com.xperiencemusic.music.theory

/** Reads absolute note names (solfège or letters) at the start of a string. */
internal object NoteParser {

    private val solfegeRoots = listOf(
        "sol" to 7, "do" to 0, "ré" to 2, "re" to 2, "mi" to 4, "fa" to 5, "la" to 9, "si" to 11,
    )
    private val letterRoots = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)

    /** Returns the pitch class and the unread remainder, or null when no note starts the text. */
    fun parseRoot(text: String): Pair<Int, String>? =
        parseSolfegeRoot(text)?.takeIf { (_, rest) -> ChordSuffix.isValid(rest) }
            ?: parseLetterRoot(text)
            ?: parseSolfegeRoot(text)

    private fun parseSolfegeRoot(text: String): Pair<Int, String>? {
        val lower = text.lowercase()
        val (prefix, pitchClass) = solfegeRoots.firstOrNull { (name, _) -> lower.startsWith(name) } ?: return null
        return withAccidental(pitchClass, text.substring(prefix.length))
    }

    private fun parseLetterRoot(text: String): Pair<Int, String>? {
        val pitchClass = text.firstOrNull()?.let(letterRoots::get) ?: return null
        return withAccidental(pitchClass, text.substring(1))
    }

    private fun withAccidental(pitchClass: Int, rest: String): Pair<Int, String> {
        val accidental = rest.firstOrNull()
        return when (accidental) {
            '#', '♯' -> (pitchClass + 1).mod(SEMITONES_PER_OCTAVE) to rest.substring(1)
            'b', '♭' -> (pitchClass - 1).mod(SEMITONES_PER_OCTAVE) to rest.substring(1)
            else -> pitchClass to rest
        }
    }
}

/** Chord qualities after the root: m, 7, maj7, sus4, add9, dim, (b9)… */
internal object ChordSuffix {
    private val pattern = Regex("""^(maj|min|dim|aug|sus|add|m|M|°|ø|\+|-|[b#♭♯]?\d+|\(|\)|,)*$""")

    fun isValid(suffix: String): Boolean = pattern.matches(suffix)
}
