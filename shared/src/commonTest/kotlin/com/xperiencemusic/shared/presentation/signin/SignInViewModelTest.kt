package com.xperiencemusic.shared.presentation.signin

import com.xperiencemusic.shared.domain.model.AppEnvironment
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.usecase.RequestSignInCodeUseCase
import com.xperiencemusic.shared.domain.usecase.SignInLinkInbox
import com.xperiencemusic.shared.domain.usecase.VerifySignInCodeUseCase
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    private val sessions = FakeSessionRepository()
    private val links = SignInLinkInbox()
    private lateinit var viewModel: SignInViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = SignInViewModel(
            RequestSignInCodeUseCase(sessions),
            VerifySignInCodeUseCase(sessions),
            AppEnvironment(isDemo = true, demoCode = "123456"),
            links,
        )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `le parcours e-mail puis code connecte le membre`() = runTest {
        viewModel.onEvent(SignInEvent.EmailChanged("Grace@Band.fr"))
        viewModel.onEvent(SignInEvent.SubmitClicked)
        assertEquals(SignInStep.CODE, viewModel.state.value.step)
        assertEquals("grace@band.fr", viewModel.state.value.email)

        viewModel.onEvent(SignInEvent.CodeChanged("12a3 4567"))
        assertEquals("123456", viewModel.state.value.code)
        viewModel.onEvent(SignInEvent.SubmitClicked)
        assertEquals(SessionState.NeedsProfile("grace@band.fr"), sessions.state.value)
    }

    @Test
    fun `le lien de l'e-mail remplit le code et connecte directement`() = runTest {
        links.receive("xperiencemusic://connexion?email=grace%40band.fr&code=123456")
        assertEquals(SessionState.NeedsProfile("grace@band.fr"), sessions.state.value)
        assertEquals("123456", viewModel.state.value.code)
        assertNull(links.link.value, "the link is used once")
    }

    @Test
    fun `un mauvais code affiche une erreur qui disparaît à la saisie`() = runTest {
        viewModel.onEvent(SignInEvent.EmailChanged("g@b.fr"))
        viewModel.onEvent(SignInEvent.SubmitClicked)
        viewModel.onEvent(SignInEvent.CodeChanged("000000"))
        viewModel.onEvent(SignInEvent.SubmitClicked)
        assertEquals(SignInError.INVALID_CODE, viewModel.state.value.error)
        viewModel.onEvent(SignInEvent.CodeChanged("00000"))
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `changer d'adresse revient à l'étape e-mail`() = runTest {
        viewModel.onEvent(SignInEvent.EmailChanged("g@b.fr"))
        viewModel.onEvent(SignInEvent.SubmitClicked)
        viewModel.onEvent(SignInEvent.ChangeEmailClicked)
        assertEquals(SignInStep.EMAIL, viewModel.state.value.step)
        assertEquals("123456", viewModel.state.value.demoCode)
    }
}
