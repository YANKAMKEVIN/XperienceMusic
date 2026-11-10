package com.xperiencemusic.shared.presentation.app

import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.data.repository.InMemoryPlanningRepository
import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.Reminder
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.repository.LocalDataStore
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObservePlanningUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveRemoteLoadFailureUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSessionUseCase
import com.xperiencemusic.shared.domain.usecase.RetryRemoteLoadsUseCase
import com.xperiencemusic.shared.domain.usecase.RetrySessionUseCase
import com.xperiencemusic.shared.domain.usecase.SignOutUseCase
import com.xperiencemusic.shared.domain.usecase.SyncServiceRemindersUseCase
import com.xperiencemusic.shared.fakes.FakeAnnouncedMonthsStore
import com.xperiencemusic.shared.fakes.FakeReminderScheduler
import com.xperiencemusic.shared.fakes.FakeRemoteLoadStatus
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.schedule
import com.xperiencemusic.shared.fakes.setlistRepository
import com.xperiencemusic.shared.fakes.songRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val sessions = FakeSessionRepository(member())
    private var wipes = 0
    private val loads = FakeRemoteLoadStatus()
    private val planning = InMemoryPlanningRepository(
        listOf(PlanningMonth(LocalDate(2026, 11, 1), null, isValidated = true)),
        listOf(ServiceSignup(LocalDate(2026, 11, 8), "me", true)),
    )
    private val reminders = FakeReminderScheduler()
    private lateinit var viewModel: AppViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = AppViewModel(
            ObserveSessionUseCase(sessions),
            RetrySessionUseCase(sessions),
            SignOutUseCase(sessions, LocalDataStore { wipes++ }),
            ObserveRemoteLoadFailureUseCase(loads),
            RetryRemoteLoadsUseCase(loads),
            SyncServiceRemindersUseCase(
                ObservePlanningUseCase(planning, InMemoryMembersRepository(sessions), ObserveCurrentMemberUseCase(sessions)),
                schedule(setlistRepository(), songRepository()),
                reminders,
                FakeAnnouncedMonthsStore(),
            ) { LocalDateTime(2026, 10, 28, 12, 0) },
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
    fun `un chargement raté affiche le bandeau jusqu'à ce que réessayer réussisse`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.hasLoadFailure.collect {} }
        assertFalse(viewModel.hasLoadFailure.value)

        loads.hasFailure.value = true
        assertTrue(viewModel.hasLoadFailure.value)

        viewModel.onEvent(AppEvent.RetryLoadsClicked)
        assertEquals(1, loads.retries)
        assertFalse(viewModel.hasLoadFailure.value)
    }

    @Test
    fun `les rappels suivent le planning tant que le membre est connecté`() = runTest {
        assertEquals(LocalDate(2026, 11, 8), reminders.scheduled?.filterIsInstance<Reminder.ServiceEve>()?.single()?.date)

        sessions.state.value = SessionState.SignedOut
        planning.answer("me", LocalDate(2026, 11, 15), true)
        assertEquals(1, reminders.scheduled?.filterIsInstance<Reminder.ServiceEve>()?.size, "nobody signed in: nothing planned again")
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
