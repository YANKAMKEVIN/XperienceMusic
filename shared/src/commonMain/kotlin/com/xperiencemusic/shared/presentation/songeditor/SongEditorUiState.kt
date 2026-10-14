package com.xperiencemusic.shared.presentation.songeditor

import com.xperiencemusic.music.chart.ChartLine
import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Mode

data class SongEditorUiState(
    val isLoading: Boolean = false,
    val isNew: Boolean = true,
    val title: String = "",
    val artist: String = "",
    val key: Key = Key(0, Mode.MAJOR),
    val chart: String = "",
    /** The chart as it will be displayed, in solfège, to check the chords were understood. */
    val preview: List<ChartLine> = emptyList(),
    /** Bracketed tokens that are not chords, likely typos. */
    val unknownChords: List<String> = emptyList(),
    /** The chart still has chords on their own line above the lyrics. */
    val hasChordsAbove: Boolean = false,
    /** A link to one accords.app song, to fill a new song from it. */
    val importLink: String = "",
    val isImporting: Boolean = false,
    val importError: SongImportError? = null,
    val isSaving: Boolean = false,
    val error: SongEditorError? = null,
    /** Set once saved; the screen then navigates away. */
    val savedSongId: String? = null,
    val isConfirmingDeletion: Boolean = false,
    val isDeleted: Boolean = false,
) {
    val canSave: Boolean get() = !isLoading && !isSaving && !isImporting && title.isNotBlank()
    val canImport: Boolean get() = isNew && !isImporting && importLink.isNotBlank()
}

enum class SongImportError { NOT_A_SONG_LINK, NOT_FOUND, NETWORK }

enum class SongEditorError { FORBIDDEN, MISSING_TITLE, NETWORK }

sealed interface SongEditorEvent {
    data class TitleChanged(val title: String) : SongEditorEvent
    data class ArtistChanged(val artist: String) : SongEditorEvent
    data class TonicSelected(val tonic: Int) : SongEditorEvent
    data class ModeSelected(val mode: Mode) : SongEditorEvent
    data class ChartChanged(val chart: String) : SongEditorEvent
    data class ImportLinkChanged(val link: String) : SongEditorEvent
    data object ImportClicked : SongEditorEvent
    data object PlaceChordsClicked : SongEditorEvent
    data object SaveClicked : SongEditorEvent
    data object DeleteClicked : SongEditorEvent
    data object DeleteConfirmed : SongEditorEvent
    data object DeleteDismissed : SongEditorEvent
}
