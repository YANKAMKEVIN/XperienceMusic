package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.domain.model.ObjectiveRecording
import com.xperiencemusic.shared.domain.model.PlaybackState
import com.xperiencemusic.shared.domain.model.PlaybackTrack

/** A take sent for an objective, as the screens list it. */
data class ObjectiveTakeUi(
    val id: String,
    val audioPath: String,
    /** 1 for the first take ever sent. */
    val number: Int,
    /** The day it was sent, "2026-10-18"; empty for a take only known on this phone. */
    val day: String,
    val seconds: Int,
    val isPlaying: Boolean,
) {
    /** What the player plays, with the labels the screen chose for the system controls. */
    fun track(title: String, subtitle: String) = PlaybackTrack(id, title, subtitle, audioPath)
}

/** The takes of [objectiveId] among [recordings] (newest first), with the one playing marked. */
fun objectiveTakes(recordings: List<ObjectiveRecording>, objectiveId: String, playback: PlaybackState): List<ObjectiveTakeUi> {
    val takes = recordings.filter { it.objectiveId == objectiveId }
    return takes.mapIndexed { index, recording ->
        val id = "objective-${recording.audioPath}"
        ObjectiveTakeUi(
            id = id,
            audioPath = recording.audioPath,
            number = takes.size - index,
            day = recording.submittedAt.take(10),
            seconds = recording.seconds,
            isPlaying = playback.track?.id == id && playback.isPlaying,
        )
    }
}
