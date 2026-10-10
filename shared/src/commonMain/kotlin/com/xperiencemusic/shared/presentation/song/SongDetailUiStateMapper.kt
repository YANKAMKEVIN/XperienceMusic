package com.xperiencemusic.shared.presentation.song

import com.xperiencemusic.music.chart.ChordChart
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.presentation.common.Formatting

/** Screen-local choices that are not stored anywhere. */
data class SongDetailLocalState(
    val transposeSemitones: Int = 0,
    val notation: Notation = Notation.NASHVILLE,
    val pendingDeletionId: String? = null,
    val message: SongMessage? = null,
)

/**
 * Pure derivation of the screen state; the ViewModel only combines its inputs.
 * The reference recording is listed first, then the most recent.
 */
fun deriveSongDetailState(song: Song?, member: Member?, local: SongDetailLocalState): SongDetailUiState {
    if (song == null) return SongDetailUiState(isLoading = false, isMissing = true)

    val displayedKey = song.key.transposedBy(local.transposeSemitones)
    // Nashville numbers do not name the key, so the key itself is always shown in solfège.
    val keyNotation = if (local.notation == Notation.NASHVILLE) Notation.SOLFEGE else local.notation
    val recordings = song.recordings
        .sortedWith(compareByDescending<Recording> { it.isReference }.thenByDescending { it.recordedOn })
        .map { it.toUi() }

    return SongDetailUiState(
        isLoading = false,
        title = song.title,
        artist = song.artist,
        keyLabel = displayedKey.label(keyNotation),
        originalKeyLabel = song.key.label(keyNotation).takeIf { displayedKey != song.key },
        notation = local.notation,
        chart = ChordChart.render(song.chart, displayedKey, local.notation),
        recordings = recordings,
        maxRecordings = Song.MAX_RECORDINGS,
        canManageRecordings = member?.role?.can(Permission.MANAGE_RECORDINGS) == true,
        voiceTracks = song.voiceTracks.map { it.voicePart },
        links = song.links.map { LinkUi(it.label, it.url) },
        pendingDeletion = recordings.firstOrNull { it.id == local.pendingDeletionId },
        message = local.message,
    )
}

private fun Recording.toUi() = RecordingUi(
    id = id,
    title = title,
    date = Formatting.shortDate(recordedOn),
    duration = Formatting.duration(durationSeconds),
    isReference = isReference,
)
