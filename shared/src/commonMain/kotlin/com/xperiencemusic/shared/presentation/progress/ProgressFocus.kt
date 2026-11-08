package com.xperiencemusic.shared.presentation.progress

import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.learning.complete
import com.xperiencemusic.shared.domain.learning.completedIn
import com.xperiencemusic.shared.domain.learning.nextIn
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.StepState

/** The one thing "Ma progression" puts forward today. */
sealed interface FocusUi {
    /** A voice the super admin sent back: the most pressing. */
    data class ReworkVoice(val songId: String, val title: String, val feedback: String?) : FocusUi
    /** The open step of the singers' microphone path. */
    data class Step(val stepId: String, val title: String, val detail: String, val tier: Int) : FocusUi
    /** The next guided-learning activity, in the first module not finished yet. */
    data class Learning(val moduleId: String, val moduleTitle: String, val activityTitle: String, val done: Int, val total: Int) : FocusUi
    /** A song of the week with no voice sent yet. */
    data class SendVoice(val songId: String, val title: String) : FocusUi
    data class WarmUp(val id: String) : FocusUi
    /** Nothing to do now: what is left waits for the super admin. */
    data object UpToDate : FocusUi
}

/** A guided-learning module with how far the member went in it. */
data class ModuleProgressUi(val id: String, val title: String, val done: Int, val total: Int, val objectiveId: String)

/** The guided modules of the member's part, in the catalogue's order; empty without a learning history. */
fun ProgressUiState.learningModules(): List<ModuleProgressUi> {
    val progress = learning ?: return emptyList()
    return LearningCatalog.modulesFor(instrument).map { module ->
        ModuleProgressUi(module.id, module.title, progress.completedIn(module).size, module.activities.size, module.objectiveId)
    }
}

/**
 * The objectives under way: not ticked yet, but with at least one activity of their guided module
 * done. Only those light up as "en cours" in the ring; an untouched objective stays plain.
 */
fun startedObjectives(objectives: List<ObjectiveUi>, modules: List<ModuleProgressUi>): Set<String> =
    objectives.filter { o -> o.status == null && modules.any { it.objectiveId == o.id && it.done > 0 } }.map { it.id }.toSet()

/**
 * What to do next, most pressing first: a voice to rework, the open microphone step, the guided
 * module of an objective still to reach, a song of the week to sing, then the daily warm-up.
 */
fun ProgressUiState.focus(): FocusUi {
    weekVoices.firstOrNull { it.status == ExerciseStatus.TO_REWORK }?.let { return FocusUi.ReworkVoice(it.songId, it.title, it.feedback) }
    tiers.firstNotNullOfOrNull { tier -> tier.steps.firstOrNull { it.state == StepState.CURRENT }?.let { tier.number to it } }
        ?.let { (tier, step) -> return FocusUi.Step(step.id, step.title, step.detail, tier) }
    learningFocus()?.let { return it }
    weekVoices.firstOrNull { it.status == null }?.let { return FocusUi.SendVoice(it.songId, it.title) }
    warmUp?.takeIf { !it.isDoneToday }?.let { return FocusUi.WarmUp(it.id) }
    return FocusUi.UpToDate
}

private fun ProgressUiState.learningFocus(): FocusUi.Learning? {
    val progress = learning ?: return null
    // Objectives already validated need no more practice; with no objectives listed, every module counts.
    val open = objectives.filter { it.status != ExerciseStatus.VALIDATED }.map { it.id }.toSet()
    val module = LearningCatalog.modulesFor(instrument)
        .filter { objectives.isEmpty() || it.objectiveId in open }
        .firstOrNull { !progress.complete(it) } ?: return null
    return FocusUi.Learning(
        module.id, module.title, module.activities[progress.nextIn(module)].title,
        progress.completedIn(module).size, module.activities.size,
    )
}
