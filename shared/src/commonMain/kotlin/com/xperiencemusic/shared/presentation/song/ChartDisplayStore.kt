package com.xperiencemusic.shared.presentation.song

import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.repository.DevicePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How this musician reads a chart, remembered on the phone and shared by every song page and the
 * stage: a pianist who reads Do-Ré-Mi chooses it once, not on each song of the setlist.
 */
class ChartDisplayStore(private val preferences: DevicePreferences) {
    private val _notation = MutableStateFlow(
        preferences.read(NOTATION)?.let { saved -> Notation.entries.firstOrNull { it.name == saved } } ?: Notation.NASHVILLE,
    )
    val notation: StateFlow<Notation> = _notation.asStateFlow()

    /** "Paroles seules"; null until the musician chooses, so a singer starts without the chords. */
    private val _lyricsOnly = MutableStateFlow(preferences.read(LYRICS_ONLY)?.toBooleanStrictOrNull())
    val lyricsOnly: StateFlow<Boolean?> = _lyricsOnly.asStateFlow()

    fun setLyricsOnly(lyricsOnly: Boolean) {
        _lyricsOnly.value = lyricsOnly
        preferences.write(LYRICS_ONLY, lyricsOnly.toString())
    }

    fun selectNotation(notation: Notation) {
        _notation.value = notation
        preferences.write(NOTATION, notation.name)
    }

    private companion object {
        const val NOTATION = "chart.notation"
        const val LYRICS_ONLY = "chart.lyricsOnly"
    }
}
