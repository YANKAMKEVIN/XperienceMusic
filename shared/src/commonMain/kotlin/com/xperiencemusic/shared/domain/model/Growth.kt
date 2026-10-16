package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.LocalDate

/** The level after this one, or null at the top. */
fun Level.next(): Level? = Level.entries.getOrNull(ordinal + 1)

/** The singers' tiers a level asks for: Confirmé the first three, Pro all five. */
fun Level.requiredSingerTiers(): IntRange = when (this) {
    Level.BEGINNER -> IntRange.EMPTY
    Level.CONFIRMED -> 1..3
    Level.PRO -> 1..5
}

/** Something a musician shows the super admin they can do, on the way to [level]. */
data class Objective(
    val id: String,
    val instrument: Instrument,
    val level: Level,
    val order: Int,
    val title: String,
    val description: String,
)

/** A musician's tick on an objective: SUBMITTED until the super admin validates it or sends it back. */
data class ObjectiveCheck(
    val memberId: String,
    val memberName: String,
    val instrument: Instrument?,
    val objectiveId: String,
    val status: ExerciseStatus,
    val feedback: String?,
    val submittedOn: LocalDate,
)

/** A member asking to move up to [targetLevel]; the super admin confirms it or sends it back. */
data class LevelRequest(
    val id: String,
    val memberId: String,
    val memberName: String,
    val instrument: Instrument?,
    val voicePart: VoicePart?,
    val targetLevel: Level,
    val status: ExerciseStatus,
    val feedback: String?,
    val submittedOn: LocalDate,
)

/** How far a member is on the way to their next level. */
data class LevelProgress(
    val current: Level,
    /** Null at the top level. */
    val next: Level?,
    val done: Int,
    val required: Int,
    /** The request for [next], once sent. */
    val request: LevelRequest?,
) {
    val isReady: Boolean get() = next != null && required > 0 && done >= required
    val canAsk: Boolean get() = isReady && (request == null || request.status == ExerciseStatus.TO_REWORK)
}

/** Where [member] stands, from their singer [path] or their musician objectives. */
fun levelProgress(
    member: Member,
    path: SingerPath?,
    objectives: List<Objective>,
    checks: List<ObjectiveCheck>,
    requests: List<LevelRequest>,
): LevelProgress {
    val current = member.profile.level
    val next = current.next()
    val request = next?.let { target -> requests.firstOrNull { it.memberId == member.id && it.targetLevel == target } }
    if (next == null) return LevelProgress(current, null, 0, 0, null)
    return if (member.profile.instrument == Instrument.SINGER) {
        val tiers = next.requiredSingerTiers()
        LevelProgress(current, next, tiers.count { path?.isTierComplete(it) == true }, tiers.count(), request)
    } else {
        val needed = objectives.filter { it.instrument == member.profile.instrument && it.level == next }
        val validated = needed.count { objective ->
            checks.any { it.memberId == member.id && it.objectiveId == objective.id && it.status == ExerciseStatus.VALIDATED }
        }
        LevelProgress(current, next, validated, needed.size, request)
    }
}
