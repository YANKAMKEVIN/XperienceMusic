package com.xperiencemusic.shared.presentation.progress

import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.shared.domain.model.ExerciseCheck
import com.xperiencemusic.shared.domain.usecase.GrowthReview
import com.xperiencemusic.shared.domain.usecase.MyGrowth
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.PathStep
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.SingerPath
import com.xperiencemusic.shared.domain.model.VoicePart
import kotlin.math.roundToInt

/** Adds the week's songs and the review queue to an already derived state. */
fun ProgressUiState.withVoices(
    weekSongs: List<Song>,
    mine: List<VoiceSubmission>,
    allSubmissions: List<VoiceSubmission>,
): ProgressUiState = copy(
    weekVoices = if (hasPath) {
        weekSongs.map { song ->
            val sent = mine.firstOrNull { it.songId == song.id }
            WeekVoiceUi(song.id, song.title, sent?.status, sent?.feedback)
        }
    } else {
        emptyList()
    },
    toReviewCount = if (canReview) allSubmissions.count { it.status == ExerciseStatus.SUBMITTED } else 0,
)

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
        canReview = member.role.can(Permission.REVIEW_TRAINING),
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

/** Adds the way to the next level, and the super admin's queue, to an already derived state. */
fun ProgressUiState.withGrowth(growth: MyGrowth?, review: GrowthReview?): ProgressUiState {
    if (growth == null) return this
    val progress = growth.progress
    return copy(
        levelCard = LevelCardUi(
            current = progress.current,
            next = progress.next,
            done = progress.done,
            required = progress.required,
            requestStatus = progress.request?.status,
            feedback = progress.request?.feedback,
            canAsk = progress.canAsk,
        ),
        objectives = growth.objectives.sortedBy { it.order }.map { objective ->
            val check = growth.checks.firstOrNull { it.objectiveId == objective.id }
            val byMicrophone = objective.autoTier?.let { tier -> growth.path?.isTierComplete(tier) == true }
            ObjectiveUi(
                id = objective.id,
                title = objective.title,
                learn = objective.learn,
                criterion = objective.criterion,
                status = when (byMicrophone) {
                    true -> ExerciseStatus.VALIDATED
                    false -> null
                    null -> check?.status
                },
                feedback = check?.feedback.takeIf { objective.autoTier == null },
                autoTier = objective.autoTier,
            )
        },
        toValidateCount = if (canReview && review != null) {
            review.requests.count { it.status == ExerciseStatus.SUBMITTED } + review.checks.count { it.status == ExerciseStatus.SUBMITTED }
        } else {
            0
        },
    )
}
