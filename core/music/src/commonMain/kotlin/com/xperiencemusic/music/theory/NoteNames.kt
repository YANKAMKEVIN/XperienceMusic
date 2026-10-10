package com.xperiencemusic.music.theory

/** Spelling of the twelve pitch classes (C = 0) in each absolute notation. */
internal object NoteNames {
    private val lettersSharp = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val lettersFlat = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")
    private val solfegeSharp = listOf("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
    private val solfegeFlat = listOf("Do", "Réb", "Ré", "Mib", "Mi", "Fa", "Solb", "Sol", "Lab", "La", "Sib", "Si")

    fun name(pitchClass: Int, notation: Notation, useFlats: Boolean): String {
        val index = pitchClass.mod(SEMITONES_PER_OCTAVE)
        return when (notation) {
            Notation.LETTERS -> if (useFlats) lettersFlat[index] else lettersSharp[index]
            // Nashville has no absolute names; callers asking for one get solfège, the band's default.
            Notation.SOLFEGE, Notation.NASHVILLE -> if (useFlats) solfegeFlat[index] else solfegeSharp[index]
        }
    }
}

const val SEMITONES_PER_OCTAVE = 12
