package com.xperiencemusic.shared.presentation.help

import com.xperiencemusic.shared.data.help.BundledHelpRepository
import com.xperiencemusic.shared.domain.model.HelpAudience
import com.xperiencemusic.shared.domain.model.HelpPage
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.usecase.ObserveHelpUseCase
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HelpStateTest {
    private val members = HelpPage(HelpAudience.MEMBERS, emptyList(), emptyList())
    private val leaders = HelpPage(HelpAudience.LEADERS, emptyList(), emptyList())

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `un membre lit sa page, sans onglets`() {
        val state = deriveHelpState(listOf(members), chosen = null)
        assertFalse(state.isLoading)
        assertFalse(state.showsTabs)
        assertEquals(HelpAudience.MEMBERS, state.page?.audience)
    }

    @Test
    fun `un responsable ouvre sa page et peut passer à celle des membres`() {
        val opened = deriveHelpState(listOf(leaders, members), chosen = null)
        assertTrue(opened.showsTabs)
        assertEquals(listOf(HelpAudience.LEADERS, HelpAudience.MEMBERS), opened.audiences)
        assertEquals(HelpAudience.LEADERS, opened.selected)
        assertEquals(HelpAudience.MEMBERS, deriveHelpState(listOf(leaders, members), HelpAudience.MEMBERS).page?.audience)
    }

    @Test
    fun `une page que le rôle ne lit plus retombe sur la sienne`() {
        assertEquals(HelpAudience.MEMBERS, deriveHelpState(listOf(members), HelpAudience.LEADERS).selected)
    }

    @Test
    fun `les pages suivent le rôle du compte connecté`() = runTest {
        val sessions = FakeSessionRepository(member(role = Role.MEMBER))
        val observe = ObserveHelpUseCase(sessions, BundledHelpRepository())
        assertEquals(listOf(HelpAudience.MEMBERS), observe().first().map { it.audience })

        sessions.state.value = SessionState.Ready(member(role = Role.CONTENT_ADMIN))
        assertEquals(listOf(HelpAudience.LEADERS, HelpAudience.MEMBERS), observe().first().map { it.audience })

        sessions.state.value = SessionState.SignedOut
        assertEquals(listOf(HelpAudience.MEMBERS), observe().first().map { it.audience })
    }

    @Test
    fun `le ViewModel change de page à la demande`() = runTest {
        val viewModel = HelpViewModel(ObserveHelpUseCase(FakeSessionRepository(member(role = Role.SUPER_ADMIN)), BundledHelpRepository()))
        val collected = mutableListOf<HelpUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect { collected += it } }
        assertEquals(HelpAudience.LEADERS, collected.last().selected)

        viewModel.onEvent(HelpEvent.AudienceSelected(HelpAudience.MEMBERS))

        assertEquals(HelpAudience.MEMBERS, collected.last().page?.audience)
    }
}
