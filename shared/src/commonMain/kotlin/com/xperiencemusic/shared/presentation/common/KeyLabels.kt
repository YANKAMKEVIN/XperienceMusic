package com.xperiencemusic.shared.presentation.common

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Mode
import com.xperiencemusic.music.theory.Notation

/** "Sol", "Lam": the key as it fits in a pill. */
fun Key.shortLabel(): String = tonicName(Notation.SOLFEGE) + if (mode == Mode.MINOR) "m" else ""

/**
 * A transposition as musicians write it: "+2", "−1" (a true minus sign, the width of the plus),
 * "0" for the song's own key.
 */
fun signedSemitones(semitones: Int): String = when {
    semitones > 0 -> "+$semitones"
    semitones < 0 -> "\u2212${-semitones}"
    else -> "0"
}
