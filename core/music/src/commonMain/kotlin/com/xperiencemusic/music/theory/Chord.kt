package com.xperiencemusic.music.theory

/**
 * A chord stored relative to the song's key, so a chart is transposed simply by rendering it in
 * another key. [suffix] carries the quality verbatim ("m", "7", "sus4", "maj7"…).
 */
data class Chord(
    val root: ScaleDegree,
    val suffix: String = "",
    val bass: ScaleDegree? = null,
) {
    fun render(key: Key, notation: Notation): String {
        val rootText = degreeText(root, key, notation)
        val bassText = bass?.let { "/" + degreeText(it, key, notation) }.orEmpty()
        return rootText + suffix + bassText
    }

    private fun degreeText(degree: ScaleDegree, key: Key, notation: Notation): String =
        if (notation == Notation.NASHVILLE) {
            degree.toString()
        } else {
            NoteNames.name(key.pitchClassOf(degree), notation, key.usesFlats)
        }

    companion object {
        private val nashvillePattern = Regex("""^([b#♭♯]?)([1-7])(.*)$""")

        /**
         * Parses a chord written in any notation — "6m", "b7", "4/1", "Rém7", "Sol/Si", "C#m" — and
         * expresses it relative to [key]. Returns null for tokens that are not chords ("N.C.", "x2").
         */
        fun parse(token: String, key: Key): Chord? {
            val text = token.trim()
            if (text.isEmpty()) return null
            val slash = text.lastIndexOf('/')
            val main = if (slash > 0) text.substring(0, slash) else text
            val bassToken = if (slash > 0) text.substring(slash + 1) else null

            val (root, suffix) = parseRoot(main, key) ?: return null
            if (!ChordSuffix.isValid(suffix)) return null
            val bass = bassToken?.let { bassText ->
                val (degree, rest) = parseRoot(bassText, key) ?: return null
                if (rest.isNotEmpty()) return null
                degree
            }
            return Chord(root, suffix, bass)
        }

        private fun parseRoot(text: String, key: Key): Pair<ScaleDegree, String>? {
            nashvillePattern.matchEntire(text)?.let { match ->
                val (accidental, number, rest) = match.destructured
                val alteration = when (accidental) {
                    "b", "♭" -> -1
                    "#", "♯" -> 1
                    else -> 0
                }
                return ScaleDegree(number.toInt(), alteration) to rest
            }
            val (pitchClass, rest) = NoteParser.parseRoot(text) ?: return null
            return key.degreeOf(pitchClass) to rest
        }
    }
}
