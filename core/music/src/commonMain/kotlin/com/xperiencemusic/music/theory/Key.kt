package com.xperiencemusic.music.theory

enum class Mode(internal val intervals: List<Int>) {
    MAJOR(listOf(0, 2, 4, 5, 7, 9, 11)),
    MINOR(listOf(0, 2, 3, 5, 7, 8, 10)),
}

/**
 * The tonality a song is played in. [tonic] is a pitch class, C = 0 … B = 11.
 */
data class Key(val tonic: Int, val mode: Mode = Mode.MAJOR) {

    init {
        require(tonic in 0 until SEMITONES_PER_OCTAVE) { "tonic must be a pitch class in 0..11, was $tonic" }
    }

    /**
     * Flat keys (F, Bb, Eb, Ab, Db and their relative minors) spell accidentals with flats, and so
     * does Eb minor, which charts write far more often than D# minor.
     */
    val usesFlats: Boolean
        get() {
            if (mode == Mode.MINOR && tonic == E_FLAT) return true
            val relativeMajor = if (mode == Mode.MAJOR) tonic else (tonic + 3).mod(SEMITONES_PER_OCTAVE)
            return relativeMajor in FLAT_MAJOR_TONICS
        }

    fun transposedBy(semitones: Int): Key = copy(tonic = (tonic + semitones).mod(SEMITONES_PER_OCTAVE))

    /** Pitch class of [degree] in this key. */
    fun pitchClassOf(degree: ScaleDegree): Int =
        (tonic + mode.intervals[degree.number - 1] + degree.alteration).mod(SEMITONES_PER_OCTAVE)

    /**
     * Scale degree of an absolute [pitchClass]. Notes outside the scale are written as a flattened
     * degree (b3, b7…), the usual worship-chart spelling, except the tritone which reads #4.
     */
    fun degreeOf(pitchClass: Int): ScaleDegree {
        val offset = (pitchClass - tonic).mod(SEMITONES_PER_OCTAVE)
        val exact = mode.intervals.indexOf(offset)
        if (exact >= 0) return ScaleDegree(exact + 1)
        if (offset == TRITONE) {
            val fourth = mode.intervals.indexOf(offset - 1)
            if (fourth >= 0) return ScaleDegree(fourth + 1, alteration = 1)
        }
        val above = mode.intervals.indexOf((offset + 1).mod(SEMITONES_PER_OCTAVE))
        return if (above >= 0) {
            ScaleDegree(above + 1, alteration = -1)
        } else {
            ScaleDegree(mode.intervals.indexOf(offset - 1) + 1, alteration = 1)
        }
    }

    /** "Sib", "Bb", "Fa#"… without the mode. */
    fun tonicName(notation: Notation): String = NoteNames.name(tonic, notation, usesFlats)

    fun label(notation: Notation): String {
        val name = tonicName(notation)
        return when (notation) {
            Notation.LETTERS -> if (mode == Mode.MINOR) "${name}m" else name
            Notation.SOLFEGE, Notation.NASHVILLE -> if (mode == Mode.MINOR) "$name mineur" else "$name majeur"
        }
    }

    companion object {
        private val FLAT_MAJOR_TONICS = setOf(1, 3, 5, 8, 10)
        private const val TRITONE = 6
        private const val E_FLAT = 3

        /** Parses "Do", "Sol#", "Bb", "Am", "Ré mineur"… Returns null when the text is not a key. */
        fun parse(text: String): Key? {
            val trimmed = text.trim()
            val isMinorWord = trimmed.endsWith("mineur", ignoreCase = true)
            val withoutWord = trimmed
                .removeSuffix("mineur").removeSuffix("Mineur")
                .removeSuffix("majeur").removeSuffix("Majeur")
                .trim()
            val (pitchClass, rest) = NoteParser.parseRoot(withoutWord) ?: return null
            val mode = when {
                isMinorWord -> Mode.MINOR
                rest == "m" || rest == "min" -> Mode.MINOR
                rest.isEmpty() -> Mode.MAJOR
                else -> return null
            }
            return Key(pitchClass, mode)
        }
    }
}

/** A degree of the scale (1..7), optionally raised (+1) or lowered (-1) by a semitone. */
data class ScaleDegree(val number: Int, val alteration: Int = 0) {
    init {
        require(number in 1..7) { "degree must be in 1..7, was $number" }
        require(alteration in -1..1) { "alteration must be -1, 0 or 1, was $alteration" }
    }

    override fun toString(): String = when (alteration) {
        -1 -> "b$number"
        1 -> "#$number"
        else -> "$number"
    }
}
