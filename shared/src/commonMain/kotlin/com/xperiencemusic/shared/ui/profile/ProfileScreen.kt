package com.xperiencemusic.shared.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.profile.ProfileEvent
import com.xperiencemusic.shared.presentation.profile.ProfileUiState
import com.xperiencemusic.shared.presentation.profile.ProfileViewModel
import com.xperiencemusic.shared.presentation.profile.deriveProfileState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.onboarding_instrument_title
import com.xperiencemusic.shared.resources.onboarding_level_title
import com.xperiencemusic.shared.resources.onboarding_name_label
import com.xperiencemusic.shared.resources.onboarding_voice_title
import com.xperiencemusic.shared.resources.profile_level_note
import com.xperiencemusic.shared.resources.profile_role_change_note
import com.xperiencemusic.shared.resources.profile_save
import com.xperiencemusic.shared.resources.profile_title
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.onboarding.RoleChoices
import com.xperiencemusic.shared.ui.onboarding.VoiceChoices
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.xperiencemusic.shared.ui.components.LoadingIndicator

@Composable
fun ProfileScreen(onBack: () -> Unit, viewModel: ProfileViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onBack() }
    ProfileScreenContent(state, viewModel::onEvent, onBack)
}

/** The welcome answers again, with the same cards; the level is shown, not chosen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(state: ProfileUiState, onEvent: (ProfileEvent) -> Unit, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.hasError) {
        if (state.hasError) {
            snackbar.showSnackbar(getString(Res.string.error_generic))
            onEvent(ProfileEvent.ErrorShown)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(0),
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                GradientButton(
                    onClick = { onEvent(ProfileEvent.SaveClicked) },
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.m),
                ) { Text(stringResource(Res.string.profile_save), style = MaterialTheme.typography.titleMedium) }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (state.isLoading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { onEvent(ProfileEvent.NameChanged(it)) },
                label = { Text(stringResource(Res.string.onboarding_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            SectionTitle(stringResource(Res.string.onboarding_instrument_title))
            RoleChoices(state.instrument) { onEvent(ProfileEvent.InstrumentSelected(it)) }
            if (state.saved != null && state.instrument != state.saved.instrument) {
                Text(stringResource(Res.string.profile_role_change_note), style = MaterialTheme.typography.bodyMedium, color = BrandColors.Amber)
            }
            if (state.isSinger) {
                SectionTitle(stringResource(Res.string.onboarding_voice_title))
                VoiceChoices(
                    selected = state.voicePart,
                    isUnknown = state.isVoiceUnknown,
                    onSelect = { onEvent(ProfileEvent.VoicePartSelected(it)) },
                    onUnknown = { onEvent(ProfileEvent.VoiceUnknownSelected) },
                )
            }
            state.level?.let { level ->
                SectionTitle(stringResource(Res.string.onboarding_level_title))
                AppCard {
                    Text(level.label(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(Res.string.profile_level_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.s).semantics { heading() })
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    AppTheme {
        ProfileScreenContent(
            deriveProfileState(MemberProfile("Sarah", Instrument.SINGER, VoicePart.ALTO, Level.BEGINNER)).copy(voicePart = VoicePart.SOPRANO),
            onEvent = {},
            onBack = {},
        )
    }
}
