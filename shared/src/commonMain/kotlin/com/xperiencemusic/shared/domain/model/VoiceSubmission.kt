package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.LocalDate

/**
 * A singer's part on a song, sent to the super admin. [status] is SUBMITTED until it is heard,
 * then VALIDATED or TO_REWORK with [feedback].
 */
data class VoiceSubmission(
    val id: String,
    val memberId: String,
    val memberName: String,
    val voicePart: VoicePart?,
    val songId: String,
    val songTitle: String,
    val audioPath: String,
    val durationSeconds: Int,
    val submittedOn: LocalDate,
    val status: ExerciseStatus,
    val feedback: String?,
)
