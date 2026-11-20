package com.xperiencemusic.shared.presentation.song

import com.xperiencemusic.music.chart.ChordChart
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.PlaybackState
import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.YouTubeLink
import com.xperiencemusic.shared.domain.model.youtubeUrl
import com.xperiencemusic.shared.presentation.common.Formatting
import com.xperiencemusic.shared.presentation.common.PlaybackStatus
import com.xperiencemusic.shared.presentation.common.statusOf

/** The choices made on the screen. [notation] and [lyricsOnly] outlive it (see [ChartDisplayStore]). */
data class SongDetailLocalState(
    val transposeSemitones: Int = 0,
    val notation: Notation = Notation.NASHVILLE,
    /** Null while the member never chose: singers then read the words alone, musicians the chords. */
    val lyricsOnly: Boolean? = null,
    val pendingDeletionId: String? = null,
    val message: SongMessage? = null,
)

/**
 * Pure derivation of the screen state; the ViewModel only combines its inputs.
 * The reference recording is listed first, then the most recent.
 */
fun deriveSongDetailState(
    song: Song?,
    member: Member?,
    local: SongDetailLocalState,
    playback: PlaybackState = PlaybackState(),
    offlinePaths: Set<String> = emptySet(),
): SongDetailUiState {
    if (song == null) return SongDetailUiState(isLoading = false, isMissing = true)

    val displayedKey = song.key.transposedBy(local.transposeSemitones)
    // Nashville numbers do not name the key, so the key itself is always shown in solfège.
    val keyNotation = if (local.notation == Notation.NASHVILLE) Notation.SOLFEGE else local.notation
    val recordings = song.recordings
        .sortedWith(compareByDescending<Recording> { it.isReference }.thenByDescending { it.recordedOn })
        .map { it.toUi(playback.statusOf(it.id), it.audioPath in offlinePaths) }
    val voiceTracks = song.voiceTracks.map { VoiceTrackUi(it.id, it.voicePart, playback.statusOf(it.id)) }
    val isSinger = member?.profile?.instrument == Instrument.SINGER
    // Null for a musician: no track is theirs.
    val myVoicePart = member?.profile?.voicePart.takeIf { isSinger }

    return SongDetailUiState(
        isLoading = false,
        title = song.title,
        artist = song.artist,
        keyLabel = displayedKey.label(keyNotation),
        keyHeading = displayedKey.label(keyNotation).let {
            if (local.notation == Notation.LETTERS) "${displayedKey.label(Notation.SOLFEGE)} ($it)" else it
        },
        originalKeyLabel = song.key.label(keyNotation).takeIf { displayedKey != song.key },
        notation = local.notation,
        lyricsOnly = local.lyricsOnly ?: isSinger,
        transposeSemitones = local.transposeSemitones,
        chart = ChordChart.render(song.chart, displayedKey, local.notation),
        recordings = recordings,
        maxRecordings = Song.MAX_RECORDINGS,
        canManageRecordings = member?.role?.can(Permission.MANAGE_RECORDINGS) == true,
        canEditSong = member?.role?.can(Permission.EDIT_SONG) == true,
        canRecordVoice = isSinger,
        voiceTracks = voiceTracks,
        reference = recordings.firstOrNull { it.isReference },
        myVoiceTrack = voiceTracks.firstOrNull { it.voicePart == myVoicePart },
        youtubeUrl = song.youtubeUrl,
        youtubeThumbnailUrl = song.youtubeUrl?.let(YouTubeLink::thumbnailUrl),
        links = song.links.filterNot { it.label == YouTubeLink.LABEL }.map { LinkUi(it.label, it.url) },
        pendingDeletion = recordings.firstOrNull { it.id == local.pendingDeletionId },
        message = local.message,
    )
}

private fun Recording.toUi(playback: PlaybackStatus, isAvailableOffline: Boolean) = RecordingUi(
    id = id,
    title = title,
    recordedOn = recordedOn,
    duration = Formatting.duration(durationSeconds),
    isReference = isReference,
    playback = playback,
    isAvailableOffline = isAvailableOffline,
)
