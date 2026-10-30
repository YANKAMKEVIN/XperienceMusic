package com.xperiencemusic.shared.presentation.profile

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.UpdateMyProfileUseCase
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `le profil s'ouvre sur les réponses actuelles et ne s'enregistre qu'après un changement`() = runTest {
        val sessions = FakeSessionRepository(member(level = Level.CONFIRMED))
        val vm = ProfileViewModel(ObserveCurrentMemberUseCase(sessions), UpdateMyProfileUseCase(sessions))
        assertEquals("Grâce", vm.state.value.name)
        assertEquals(VoicePart.ALTO, vm.state.value.voicePart)
        assertFalse(vm.state.value.canSave)

        vm.onEvent(ProfileEvent.NameChanged(" Grâce M. "))
        vm.onEvent(ProfileEvent.VoicePartSelected(VoicePart.SOPRANO))
        assertTrue(vm.state.value.canSave)
        vm.onEvent(ProfileEvent.SaveClicked)

        assertTrue(vm.state.value.isDone)
        val saved = sessions.currentMember()!!.profile
        assertEquals("Grâce M.", saved.displayName)
        assertEquals(VoicePart.SOPRANO, saved.voicePart)
        // The level is the leader's: it goes back unchanged, as the server requires.
        assertEquals(Level.CONFIRMED, saved.level)
    }

    @Test
    fun `un musicien n'a pas de voix, un chantre peut ne pas la connaître`() = runTest {
        val sessions = FakeSessionRepository(member())
        val vm = ProfileViewModel(ObserveCurrentMemberUseCase(sessions), UpdateMyProfileUseCase(sessions))
        vm.onEvent(ProfileEvent.VoiceUnknownSelected)
        vm.onEvent(ProfileEvent.SaveClicked)
        assertNull(sessions.currentMember()!!.profile.voicePart)

        val again = ProfileViewModel(ObserveCurrentMemberUseCase(sessions), UpdateMyProfileUseCase(sessions))
        assertTrue(again.state.value.isVoiceUnknown)
        again.onEvent(ProfileEvent.InstrumentSelected(Instrument.PIANO))
        again.onEvent(ProfileEvent.SaveClicked)
        assertEquals(Instrument.PIANO, sessions.currentMember()!!.profile.instrument)
        assertNull(sessions.currentMember()!!.profile.voicePart)
    }

    @Test
    fun `un prénom vide ne s'enregistre pas`() = runTest {
        val sessions = FakeSessionRepository(member())
        val vm = ProfileViewModel(ObserveCurrentMemberUseCase(sessions), UpdateMyProfileUseCase(sessions))
        vm.onEvent(ProfileEvent.NameChanged("  "))
        assertFalse(vm.state.value.canSave)
        vm.onEvent(ProfileEvent.SaveClicked)
        assertEquals("Grâce", sessions.currentMember()!!.profile.displayName)
    }
}
