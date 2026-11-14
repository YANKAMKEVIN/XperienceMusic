package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.domain.model.LearningPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class LearningSessionTest {
    @BeforeTest
    fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun interruptionRestoresSelectionHintsAndPreferencesWithoutPenalty() = runTest {
        val world = LearningWorld(backgroundScope)
        val first = world.viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { first.state.collect() }
        first.onEvent(LearningEvent.Check)
        first.onEvent(LearningEvent.Next)
        val preferences = LearningPreferences(touchSound = false, rewardSound = false, haptics = false, weeklyTarget = 3, startingPoint = "piano-chart")
        first.onEvent(LearningEvent.Preferences(preferences))
        first.onEvent(LearningEvent.Key(60))
        first.onEvent(LearningEvent.Hint)
        first.onEvent(LearningEvent.Home)

        val next = world.viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { next.state.collect() }
        next.onEvent(LearningEvent.Resume)
        assertEquals(1, next.state.value.index)
        assertEquals(setOf(60), next.state.value.notes)
        assertEquals(1, next.state.value.hints)
        assertFalse(next.state.value.preferences.touchSound)
        assertEquals(3, next.state.value.preferences.weeklyTarget)
        assertEquals("piano-chart", next.state.value.preferences.startingPoint)
        assertEquals(1, world.repository.attempts.size)
        next.onEvent(LearningEvent.Key(64))
        next.onEvent(LearningEvent.Key(67))
        next.onEvent(LearningEvent.Check)
        assertEquals(true, next.state.value.result)
        assertEquals(1, world.repository.attempts.last().hints)
        assertEquals(setOf(60, 64, 67), next.state.value.notes)
    }
}
