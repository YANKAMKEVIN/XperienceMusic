package com.xperiencemusic.shared.presentation.app

import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.repository.LocalDataStore
import com.xperiencemusic.shared.domain.usecase.ObserveSessionUseCase
import com.xperiencemusic.shared.domain.usecase.RetrySessionUseCase
import com.xperiencemusic.shared.domain.usecase.SignOutUseCase
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val sessions = FakeSessionRepository(member())
    private var wipes = 0
    private lateinit var viewModel: AppViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = AppViewModel(
            ObserveSessionUseCase(sessions),
            RetrySessionUseCase(sessions),
            SignOutUseCase(sessions, LocalDataStore { wipes++ }),
        )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `se déconnecter demande confirmation avant d'effacer le téléphone`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.onEvent(AppEvent.SignOutClicked)
        assertTrue(viewModel.isConfirmingSignOut.value)
        assertEquals(AppUiState.Ready, viewModel.state.value)
        assertEquals(0, wipes)

        viewModel.onEvent(AppEvent.SignOutDismissed)
        assertFalse(viewModel.isConfirmingSignOut.value)
        assertEquals(AppUiState.Ready, viewModel.state.value)

        viewModel.onEvent(AppEvent.SignOutClicked)
        viewModel.onEvent(AppEvent.SignOutConfirmed)
        assertFalse(viewModel.isConfirmingSignOut.value)
        assertEquals(AppUiState.SignIn, viewModel.state.value)
        assertEquals(1, wipes)
    }

    @Test
    fun `quitter l'accueil se fait sans confirmation`() = runTest {
        sessions.state.value = SessionState.NeedsProfile("x@y.fr")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.onEvent(AppEvent.SignOutClicked)
        assertFalse(viewModel.isConfirmingSignOut.value)
        assertEquals(AppUiState.SignIn, viewModel.state.value)
    }

    @Test
    fun `seuls les écrans d'un membre entré dans l'app confirment la déconnexion`() {
        assertTrue(AppUiState.Ready.confirmsSignOut)
        assertTrue(AppUiState.Unreachable.confirmsSignOut)
        assertFalse(AppUiState.Onboarding.confirmsSignOut)
        assertFalse(AppUiState.NotInvited("x@y.fr").confirmsSignOut)
    }

    @Test
    fun `chaque écran racine a sa propre destination`() {
        val states = listOf(
            AppUiState.Loading, AppUiState.SignIn, AppUiState.Unreachable,
            AppUiState.NotInvited("x@y.fr"), AppUiState.Onboarding, AppUiState.Ready,
        )
        assertEquals(states.size, states.map { it.destination }.toSet().size)
        assertEquals(AppUiState.NotInvited("a@b.fr").destination, AppUiState.NotInvited("x@y.fr").destination)
        assertNotEquals(AppUiState.SignIn.destination, AppUiState.Onboarding.destination)
    }
}
