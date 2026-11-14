package com.xperiencemusic.shared.presentation.progress

import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.model.LearningProgress
import com.xperiencemusic.shared.domain.learning.PianoBasics
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.StepState
import kotlinx.datetime.LocalDate
import kotlin.test.*

class ProgressFocusTest {
    private val singer = ProgressUiState(isLoading = false, instrument = Instrument.SINGER, hasPath = true)
    private fun objective(id: String, status: ExerciseStatus? = null) = ObjectiveUi(id, id, "", "", status, null)
    private fun step(id: String, state: StepState) = StepUi(id, id, "Ré4 · 5 s", state, false)

    @Test fun aVoiceToReworkComesFirst() {
        val state = singer.copy(
            weekVoices = listOf(WeekVoiceUi("s1", "Tu es saint", ExerciseStatus.TO_REWORK, "Plus juste au refrain")),
            tiers = listOf(TierUi(1, false, listOf(step("a", StepState.CURRENT)))),
        )
        assertEquals(FocusUi.ReworkVoice("s1", "Tu es saint", "Plus juste au refrain"), state.focus())
    }

    @Test fun theOpenMicrophoneStepComesBeforeLearning() {
        val state = singer.copy(
            learning = LearningProgress(),
            tiers = listOf(TierUi(1, true, listOf(step("a", StepState.DONE))), TierUi(2, false, listOf(step("b", StepState.CURRENT), step("c", StepState.LOCKED)))),
        )
        assertEquals(FocusUi.Step("b", "b", "Ré4 · 5 s", 2), state.focus())
    }

    @Test fun learningSkipsModulesOfValidatedObjectives() {
        val state = singer.copy(
            learning = LearningProgress(),
            objectives = listOf(objective("singer-c4", ExerciseStatus.VALIDATED), objective("singer-c5")),
        )
        val focus = assertIs<FocusUi.Learning>(state.focus())
        assertEquals("singer-mic", focus.moduleId)
        assertEquals(0, focus.done)
    }

    @Test fun finishedModulesWaitForTheSuperAdmin() {
        val module = LearningCatalog.module("singer-rhythm")
        val attempts = module.activities.map { LearningAttempt(it.id, "me", it.id, PianoBasics.VERSION, true, LocalDate(2026, 10, 19)) }
        val state = singer.copy(
            learning = LearningProgress(attempts),
            objectives = listOf(objective("singer-c4")),
            weekVoices = listOf(WeekVoiceUi("s1", "Tu es saint", ExerciseStatus.SUBMITTED, null)),
            warmUp = WarmUpUi("w", isDoneToday = true),
        )
        assertEquals(FocusUi.UpToDate, state.focus())
    }

    @Test fun thenASongToSendThenTheWarmUp() {
        val toSend = singer.copy(weekVoices = listOf(WeekVoiceUi("s2", "Saint Esprit", null, null)), warmUp = WarmUpUi("w", false))
        assertEquals(FocusUi.SendVoice("s2", "Saint Esprit"), toSend.focus())
        assertEquals(FocusUi.WarmUp("w"), toSend.copy(weekVoices = emptyList()).focus())
    }

    @Test fun modulesFollowThePart() {
        val piano = ProgressUiState(isLoading = false, instrument = Instrument.PIANO, learning = LearningProgress())
        assertEquals(LearningCatalog.modulesFor(Instrument.PIANO).map { it.id }, piano.learningModules().map { it.id })
        assertTrue(piano.copy(learning = null).learningModules().isEmpty())
    }

    @Test fun onlyObjectivesWithWorkDoneCountAsStarted() {
        val modules = listOf(
            ModuleProgressUi("m4", "Rythme", done = 2, total = 6, objectiveId = "singer-c4"),
            ModuleProgressUi("m5", "Micro", done = 0, total = 6, objectiveId = "singer-c5"),
            ModuleProgressUi("m6", "Tenir", done = 6, total = 6, objectiveId = "singer-c6"),
        )
        val objectives = listOf(objective("singer-c4"), objective("singer-c5"), objective("singer-c6", ExerciseStatus.SUBMITTED), objective("singer-c7"))
        assertEquals(setOf("singer-c4"), startedObjectives(objectives, modules))
        assertEquals(emptySet(), startedObjectives(objectives, emptyList()))
    }
}
