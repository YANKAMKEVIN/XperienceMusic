package com.xperiencemusic.shared.presentation.onboarding

import app.cash.turbine.test
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.usecase.CompleteOnboardingUseCase
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val sessions = FakeSessionRepository()
    private lateinit var viewModel: OnboardingViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = OnboardingViewModel(CompleteOnboardingUseCase(sessions))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `un chantre doit choisir sa voix avant de pouvoir valider`() = runTest {
        viewModel.state.test {
            assertFalse(awaitItem().canSubmit)
            viewModel.onEvent(OnboardingEvent.NameChanged("Grâce"))
            viewModel.onEvent(OnboardingEvent.InstrumentSelected(Instrument.SINGER))
            viewModel.onEvent(OnboardingEvent.LevelSelected(Level.BEGINNER))
            skipItems(2)
            val withoutVoice = awaitItem()
            assertTrue(withoutVoice.isVoicePartVisible)
            assertFalse(withoutVoice.canSubmit)
            viewModel.onEvent(OnboardingEvent.VoicePartSelected(VoicePart.ALTO))
            assertTrue(awaitItem().canSubmit)
        }
    }

    @Test
    fun `un musicien passe la question de la voix et valide à la dernière étape`() = runTest {
        viewModel.onEvent(OnboardingEvent.ContinueClicked)
        assertEquals(OnboardingStep.NAME, viewModel.state.value.step, "a name is needed first")
        viewModel.onEvent(OnboardingEvent.NameChanged(" Daniel "))
        viewModel.onEvent(OnboardingEvent.ContinueClicked)
        assertEquals(OnboardingStep.ROLE, viewModel.state.value.step)
        viewModel.onEvent(OnboardingEvent.InstrumentSelected(Instrument.PIANO))
        viewModel.onEvent(OnboardingEvent.ContinueClicked)
        assertEquals(OnboardingStep.LEVEL, viewModel.state.value.step)
        assertEquals(listOf(OnboardingStep.NAME, OnboardingStep.ROLE, OnboardingStep.LEVEL), viewModel.state.value.steps)
        viewModel.onEvent(OnboardingEvent.LevelSelected(Level.PRO))
        viewModel.onEvent(OnboardingEvent.ContinueClicked)
        assertEquals(Instrument.PIANO, sessions.currentMember()?.profile?.instrument)
        assertEquals("Daniel", sessions.currentMember()?.profile?.displayName)
    }

    @Test
    fun `un chantre répond à quatre questions et peut revenir en arrière`() = runTest {
        viewModel.onEvent(OnboardingEvent.NameChanged("Grâce"))
        viewModel.onEvent(OnboardingEvent.ContinueClicked)
        viewModel.onEvent(OnboardingEvent.InstrumentSelected(Instrument.SINGER))
        viewModel.onEvent(OnboardingEvent.ContinueClicked)
        assertEquals(OnboardingStep.VOICE, viewModel.state.value.step)
        assertEquals(2, viewModel.state.value.stepIndex)
        viewModel.onEvent(OnboardingEvent.BackClicked)
        assertEquals(OnboardingStep.ROLE, viewModel.state.value.step)
    }
}
