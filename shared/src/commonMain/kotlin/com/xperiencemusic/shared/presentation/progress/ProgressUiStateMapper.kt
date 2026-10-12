package com.xperiencemusic.shared.presentation.progress

import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.shared.domain.model.ExerciseCheck
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.PathStep
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.SingerPath
import com.xperiencemusic.shared.domain.model.VoicePart
import kotlin.math.roundToInt

fun deriveProgressState(member: Member?, path: SingerPath?): ProgressUiState {
    if (member == null) return ProgressUiState()
    val profile = member.profile
    val isSinger = profile.instrument == Instrument.SINGER
    // A singer who skipped the voice question gets exercises pitched for a mid-range voice.
    val voicePart = profile.voicePart ?: VoicePart.ALTO
    val singerPath = path.takeIf { isSinger }
    return ProgressUiState(
        isLoading = false,
        name = profile.displayName,
        instrument = profile.instrument,
        level = profile.level,
        voicePart = profile.voicePart.takeIf { isSinger },
        hasPath = isSinger,
        streakDays = singerPath?.streakDays ?: 0,
        completedTiers = singerPath?.completedTiers ?: 0,
        warmUp = singerPath?.warmUp?.let { WarmUpUi(it.id, isDoneToday = singerPath.hasPractisedToday) },
        tiers = singerPath?.let { p ->
            p.tiers.map { tier ->
                TierUi(
                    number = tier,
                    isComplete = p.isTierComplete(tier),
                    steps = p.steps.filter { it.progress.exercise.tier == tier }.map { it.toUi(voicePart) },
                )
            }
        }.orEmpty(),
        canManageMembers = member.role.can(Permission.MANAGE_MEMBERS),
    )
}

private fun PathStep.toUi(voicePart: VoicePart): StepUi {
    val exercise = progress.exercise
    return StepUi(
        id = exercise.id,
        title = exercise.title,
        detail = when (val check = exercise.check) {
            is ExerciseCheck.HoldNote -> "${Pitch.noteLabel(check.targetMidi(voicePart))} · ${check.seconds.roundToInt()} s"
            is ExerciseCheck.NoteSequence -> "${check.semitonesAboveComfort.size} notes"
            ExerciseCheck.Review -> ""
        },
        state = state,
        isChallenge = exercise.isChallenge,
    )
}
