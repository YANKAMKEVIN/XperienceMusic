package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.LocalDate

/** The level after this one, or null at the top. */
fun Level.next(): Level? = Level.entries.getOrNull(ordinal + 1)

/**
 * One module of the progression programme, on the way to [level]: what it teaches ([learn], the
 * help sheet) and what the super admin checks ([criterion]). A singer's objective with an
 * [autoTier] is validated by finishing that microphone tier instead.
 */
data class Objective(
    val id: String,
    val instrument: Instrument,
    val level: Level,
    val order: Int,
    val title: String,
    val learn: String,
    val criterion: String,
    val autoTier: Int? = null,
)

/** True once [memberId] has [objective]: its microphone tier is done, or the super admin validated it. */
fun Objective.isReached(memberId: String, path: SingerPath?, checks: List<ObjectiveCheck>): Boolean =
    autoTier?.let { path?.isTierComplete(it) == true }
        ?: checks.any { it.memberId == memberId && it.objectiveId == id && it.status == ExerciseStatus.VALIDATED }

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

/** Where [member] stands: the objectives of their next level reached, out of all of them. */
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
    val needed = objectives.filter { it.instrument == member.profile.instrument && it.level == next }
    return LevelProgress(current, next, needed.count { it.isReached(member.id, path, checks) }, needed.size, request)
}
