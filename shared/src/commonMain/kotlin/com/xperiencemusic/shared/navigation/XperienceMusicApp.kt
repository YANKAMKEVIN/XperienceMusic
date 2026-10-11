package com.xperiencemusic.shared.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.app.AppEvent
import com.xperiencemusic.shared.presentation.app.AppUiState
import com.xperiencemusic.shared.presentation.app.AppViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.session_not_invited_text
import com.xperiencemusic.shared.resources.session_not_invited_title
import com.xperiencemusic.shared.resources.session_retry
import com.xperiencemusic.shared.resources.session_sign_out
import com.xperiencemusic.shared.resources.session_unreachable_text
import com.xperiencemusic.shared.resources.session_unreachable_title
import com.xperiencemusic.shared.ui.brand.BrandSplash
import com.xperiencemusic.shared.ui.onboarding.OnboardingScreen
import com.xperiencemusic.shared.ui.session.SessionMessageScreen
import com.xperiencemusic.shared.ui.signin.SignInScreen
import com.xperiencemusic.shared.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Shared entry point of both apps. Koin must be started first (see `initKoin`). */
@Composable
fun XperienceMusicApp() {
    AppTheme {
        Surface(Modifier.fillMaxSize()) {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot(viewModel: AppViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signOut = { viewModel.onEvent(AppEvent.SignOutClicked) }
    when (val current = state) {
        AppUiState.Loading -> BrandSplash()
        AppUiState.SignIn -> SignInScreen()
        AppUiState.Unreachable -> SessionMessageScreen(
            title = stringResource(Res.string.session_unreachable_title),
            text = stringResource(Res.string.session_unreachable_text),
            primaryLabel = stringResource(Res.string.session_retry),
            onPrimary = { viewModel.onEvent(AppEvent.RetryClicked) },
            signOutLabel = stringResource(Res.string.session_sign_out),
            onSignOut = signOut,
        )
        is AppUiState.NotInvited -> SessionMessageScreen(
            title = stringResource(Res.string.session_not_invited_title),
            text = stringResource(Res.string.session_not_invited_text, current.email),
            primaryLabel = null,
            onPrimary = {},
            signOutLabel = stringResource(Res.string.session_sign_out),
            onSignOut = signOut,
        )
        AppUiState.Onboarding -> OnboardingScreen()
        AppUiState.Ready -> MainScaffold(onSignOut = signOut)
    }
}
