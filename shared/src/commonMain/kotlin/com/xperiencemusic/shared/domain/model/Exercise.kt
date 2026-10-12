package com.xperiencemusic.shared.domain.model

/**
 * One step of a singer's path ("Ma progression"). Steps are grouped in [tier]s that unlock one
 * after the other; tier [WARM_UP_TIER] holds the daily warm-up, outside the path.
 */
data class Exercise(
    val id: String,
    val tier: Int,
    val order: Int,
    val title: String,
    val instructions: String,
    val check: ExerciseCheck,
    /** The last step of a tier, going over what it taught. */
    val isChallenge: Boolean = false,
) {
    companion object {
        const val WARM_UP_TIER = 0
    }
}

/** How an exercise is validated. */
sealed interface ExerciseCheck {
    /**
     * Validated automatically by pitch detection. The target is relative to the singer's
     * [VoicePart.comfortMidi], so one exercise fits every voice. [accompanimentSemitones] plays
     * another voice's note while the singer holds theirs.
     */
    data class HoldNote(
        val semitonesAboveComfort: Int,
        val seconds: Double,
        val accompanimentSemitones: Int? = null,
    ) : ExerciseCheck {
        fun targetMidi(voicePart: VoicePart): Int = voicePart.comfortMidi + semitonesAboveComfort
        fun accompanimentMidi(voicePart: VoicePart): Int? = accompanimentSemitones?.let { voicePart.comfortMidi + it }
    }

    /** Several notes in a row (an interval, a scale), each held [secondsEach], checked like a hold. */
    data class NoteSequence(val semitonesAboveComfort: List<Int>, val secondsEach: Double) : ExerciseCheck {
        fun targetMidis(voicePart: VoicePart): List<Int> = semitonesAboveComfort.map { voicePart.comfortMidi + it }
    }

    /** The singer sends a recording that a [Role.SUPER_ADMIN] reviews. */
    data object Review : ExerciseCheck
}

enum class ExerciseStatus { TODO, SUBMITTED, VALIDATED, TO_REWORK }

data class ExerciseProgress(val exercise: Exercise, val status: ExerciseStatus)

enum class StepState { DONE, CURRENT, LOCKED }

data class PathStep(val progress: ExerciseProgress, val state: StepState)

/**
 * The singer's path: every step in order, only the first unfinished one open. [streakDays] counts
 * the days of practice in a row (see `practiceStreak`).
 */
data class SingerPath(
    val steps: List<PathStep>,
    val warmUp: Exercise?,
    val streakDays: Int,
    val hasPractisedToday: Boolean = false,
) {
    val tiers: List<Int> get() = steps.map { it.progress.exercise.tier }.distinct()

    fun isTierComplete(tier: Int): Boolean =
        steps.filter { it.progress.exercise.tier == tier }.let { it.isNotEmpty() && it.all { step -> step.state == StepState.DONE } }

    val completedTiers: Int get() = tiers.count(::isTierComplete)

    val current: PathStep? get() = steps.firstOrNull { it.state == StepState.CURRENT }

    companion object {
        /** Opens the first step not validated yet; everything before it is done, after it locked. */
        fun of(
            exercises: List<ExerciseProgress>,
            warmUp: Exercise?,
            streakDays: Int,
            hasPractisedToday: Boolean = false,
        ): SingerPath {
            val ordered = exercises.sortedWith(compareBy({ it.exercise.tier }, { it.exercise.order }))
            val firstOpen = ordered.indexOfFirst { it.status != ExerciseStatus.VALIDATED }
            val steps = ordered.mapIndexed { index, progress ->
                PathStep(
                    progress,
                    when {
                        progress.status == ExerciseStatus.VALIDATED -> StepState.DONE
                        index == firstOpen -> StepState.CURRENT
                        else -> StepState.LOCKED
                    },
                )
            }
            return SingerPath(steps, warmUp, streakDays, hasPractisedToday)
        }
    }
}
