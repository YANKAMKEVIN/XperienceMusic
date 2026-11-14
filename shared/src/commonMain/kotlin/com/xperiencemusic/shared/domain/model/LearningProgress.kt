package com.xperiencemusic.shared.domain.model

import com.xperiencemusic.shared.domain.learning.PianoBasics
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/** A member's attempts; [pending] counts those not sent to the server yet. */
data class LearningProgress(
    val attempts: List<LearningAttempt> = emptyList(),
    val pending: Int = 0,
) {
    val completed: Set<String>
        get() = attempts
            .filter { attempt ->
                attempt.passed &&
                    attempt.version == PianoBasics.VERSION &&
                    PianoBasics.activities.any { it.id == attempt.activityId }
            }
            .map { it.activityId }
            .toSet()

    val isComplete: Boolean get() = PianoBasics.activities.all { it.id in completed }

    fun reviewDue(today: LocalDate): Boolean {
        val last = attempts
            .filter { it.passed && it.activityId == PianoBasics.REVIEW_ID && it.version == PianoBasics.VERSION }
            .maxOfOrNull { it.day }
            ?: return false
        return today >= last.plus(DatePeriod(days = 3))
    }
}

/** How far a member went in a module, as the screens say it. */
enum class Mastery { DISCOVER, SUBMITTED, PRACTISING, CONSOLIDATING, VALIDATED }

fun LearningProgress.completedIn(module: LearningModule): Set<String> = module.activities
    .filter { activity ->
        attempts.any { it.activityId == activity.id && it.version == PianoBasics.VERSION && it.passed }
    }
    .map { it.id }
    .toSet()

fun LearningProgress.nextIn(module: LearningModule): Int {
    val done = completedIn(module)
    val next = module.activities.indexOfFirst { it.id !in done }
    return if (next < 0) module.activities.lastIndex else next
}

/**
 * The furthest step open to the member: the first activity neither done nor set aside for later.
 * Setting a sung activity aside never counts as a success; when only set-aside ones remain, the
 * member is brought back to the first of them.
 */
fun LearningProgress.nextIn(module: LearningModule, postponed: Set<String>): Int {
    val done = completedIn(module)
    val open = module.activities.indexOfFirst { it.id !in done && it.id !in postponed }
    return if (open >= 0) open else nextIn(module)
}

/** Only sung activities can wait: they need a microphone, earphones or a quiet room. */
fun LearningActivity.canPostpone() = kind == LearningKind.SING

fun LearningProgress.complete(module: LearningModule) = completedIn(module).size == module.activities.size

/** Same-day repetitions do not artificially increase the spacing. Hint-assisted successes return sooner. */
fun LearningProgress.reviewDate(activityId: String): LocalDate? {
    val history = attempts.filter { it.activityId == activityId && it.version == PianoBasics.VERSION }
    val last = history.lastOrNull() ?: return null
    val streak = history.asReversed()
        .takeWhile { it.passed && it.hints == 0 }
        .map { it.day }
        .distinct()
        .size
    val days = when {
        !last.passed || last.hints > 0 -> 1
        streak >= 4 -> 30
        streak == 3 -> 14
        streak == 2 -> 7
        else -> 3
    }
    return last.day.plus(DatePeriod(days = days))
}

fun LearningProgress.dueIn(module: LearningModule, today: LocalDate): List<LearningActivity> = module.activities
    .filter { activity ->
        activity.kind !in listOf(LearningKind.LESSON, LearningKind.PRACTICE) &&
            reviewDate(activity.id)?.let { due -> due <= today } == true
    }
    .sortedBy { reviewDate(it.id) }

fun LearningProgress.dailyPlan(module: LearningModule, today: LocalDate, postponed: Set<String> = emptySet()): List<Int> {
    val start = nextIn(module, postponed)
    val discovery = if (complete(module)) {
        emptyList()
    } else {
        (start..module.activities.lastIndex)
            .filter { it == start || module.activities[it].id !in postponed }
            .take(3)
    }
    val revision = dueIn(module, today).map { module.activities.indexOf(it) }.take(1)
    return (revision + discovery).distinct().ifEmpty { listOf(module.activities.lastIndex) }
}

/** The plan of today's session: a single short revision for a member coming back after a break. */
fun LearningProgress.sessionPlan(module: LearningModule, today: LocalDate, postponed: Set<String>): List<Int> {
    val plan = dailyPlan(module, today, postponed)
    return if (returning(today)) plan.take(1) else plan
}

fun LearningProgress.mastery(module: LearningModule, status: ExerciseStatus?, today: LocalDate): Mastery {
    if (status == ExerciseStatus.VALIDATED) return Mastery.VALIDATED
    val latest = attempts
        .filter { attempt -> attempt.version == PianoBasics.VERSION && module.activities.any { it.id == attempt.activityId } }
        .groupBy { it.activityId }
        .values
        .map { it.last() }
    if (latest.any { !it.passed || it.hints > 0 }) return Mastery.CONSOLIDATING
    if (status == ExerciseStatus.TO_REWORK || complete(module) || dueIn(module, today).isNotEmpty()) return Mastery.CONSOLIDATING
    if (completedIn(module).isNotEmpty()) return Mastery.PRACTISING
    return if (status == ExerciseStatus.SUBMITTED) Mastery.SUBMITTED else Mastery.DISCOVER
}

fun LearningProgress.weeklyDays(today: LocalDate): Int = attempts
    .map { it.day }
    .distinct()
    .count { it.daysUntil(today) in 0..6 }

fun LearningProgress.returning(today: LocalDate): Boolean =
    attempts.maxOfOrNull { it.day }?.daysUntil(today)?.let { it >= 7 } == true
