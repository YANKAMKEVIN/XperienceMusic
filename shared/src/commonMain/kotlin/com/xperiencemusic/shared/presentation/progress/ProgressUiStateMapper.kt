package com.xperiencemusic.shared.presentation.progress

import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.shared.domain.model.ExerciseCheck
import com.xperiencemusic.shared.domain.model.ExerciseProgress
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.VoicePart
import kotlin.math.roundToInt

fun deriveProgressState(member: Member?, path: List<ExerciseProgress>): ProgressUiState {
    if (member == null) return ProgressUiState()
    val profile = member.profile
    val isSinger = profile.instrument == Instrument.SINGER
    // A singer who skipped the voice question gets exercises pitched for a mid-range voice.
    val voicePart = profile.voicePart ?: VoicePart.ALTO
    val exercises = if (isSinger) {
        path.mapIndexed { index, progress -> progress.toUi(index + 1, voicePart) }
    } else {
        emptyList()
    }
    return ProgressUiState(
        isLoading = false,
        name = profile.displayName,
        instrument = profile.instrument,
        level = profile.level,
        hasPath = isSinger,
        exercises = exercises,
        validatedCount = exercises.count { it.status == ExerciseStatus.VALIDATED },
        canManageMembers = member.role.can(Permission.MANAGE_MEMBERS),
    )
}

private fun ExerciseProgress.toUi(position: Int, voicePart: VoicePart) = ExerciseUi(
    id = exercise.id,
    position = position,
    title = exercise.title,
    instructions = exercise.instructions,
    check = when (val check = exercise.check) {
        is ExerciseCheck.HoldNote ->
            ExerciseCheckUi.HoldNote(Pitch.noteLabel(check.targetMidi(voicePart)), check.seconds.roundToInt())
        ExerciseCheck.Review -> ExerciseCheckUi.Review
    },
    status = status,
)
