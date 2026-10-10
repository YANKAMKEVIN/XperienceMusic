package com.xperiencemusic.shared.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.presentation.app.AppUiState
import com.xperiencemusic.shared.presentation.app.AppViewModel
import com.xperiencemusic.shared.ui.onboarding.OnboardingScreen
import com.xperiencemusic.shared.ui.theme.AppTheme
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
    when (state) {
        AppUiState.LOADING -> Unit
        AppUiState.NEEDS_ONBOARDING -> OnboardingScreen()
        AppUiState.READY -> MainScaffold()
    }
}
