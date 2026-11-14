package com.xperiencemusic.shared.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.Avatar
import com.xperiencemusic.shared.domain.model.AvatarPreset
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.Role as MemberRole
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.account.AccountError
import com.xperiencemusic.shared.presentation.account.AccountEvent
import com.xperiencemusic.shared.presentation.account.AccountUiState
import com.xperiencemusic.shared.presentation.account.AccountViewModel
import com.xperiencemusic.shared.presentation.account.deriveAccountState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.account_avatar_label
import com.xperiencemusic.shared.resources.account_change_avatar
import com.xperiencemusic.shared.resources.account_edit_profile
import com.xperiencemusic.shared.resources.account_members
import com.xperiencemusic.shared.resources.account_review
import com.xperiencemusic.shared.resources.account_role_line
import com.xperiencemusic.shared.resources.account_title
import com.xperiencemusic.shared.resources.account_tools
import com.xperiencemusic.shared.resources.account_validate
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.avatar_error_photo
import com.xperiencemusic.shared.resources.avatar_error_save
import com.xperiencemusic.shared.resources.avatar_saving
import com.xperiencemusic.shared.resources.session_sign_out
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.LoadingIndicator
import com.xperiencemusic.shared.ui.components.MemberAvatar
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.platform.rememberPhotoPicker
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Sizes
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The space between the avatar and its gradient ring. */
private val AVATAR_RING_GAP = 5.dp
/** Round ripple of the avatar, ring included. */
private val AVATAR_RIPPLE_RADIUS = 64.dp
private val EDIT_BADGE_SIZE = 34.dp
/** The background-coloured ring that cuts the badge out of the avatar. */
private val EDIT_BADGE_RING = 3.dp

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onReview: () -> Unit,
    onValidate: () -> Unit,
    onManageMembers: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Cancelling the picker sends nothing: the avatar simply stays as it was.
    val pickPhoto = rememberPhotoPicker { jpeg -> viewModel.onEvent(AccountEvent.PhotoPicked(jpeg)) }
    AccountScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onPickPhoto = pickPhoto,
        onBack = onBack,
        onEditProfile = onEditProfile,
        onReview = onReview,
        onValidate = onValidate,
        onManageMembers = onManageMembers,
        onSignOut = onSignOut,
    )
}

/** Laid out like a social profile: the circle, the name, two buttons, then the member's tools. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreenContent(
    state: AccountUiState,
    onEvent: (AccountEvent) -> Unit,
    onPickPhoto: () -> Unit,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onReview: () -> Unit,
    onValidate: () -> Unit,
    onManageMembers: () -> Unit,
    onSignOut: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    var isPicking by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        snackbar.showSnackbar(getString(if (error == AccountError.PHOTO_UNREADABLE) Res.string.avatar_error_photo else Res.string.avatar_error_save))
        onEvent(AccountEvent.ErrorShown)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.account_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(0),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (state.isLoading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            BigAvatar(state, onClick = { isPicking = true })
            Identity(state)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                OutlinedButton(onClick = onEditProfile, modifier = Modifier.weight(1f).heightIn(min = Sizes.touchTarget)) {
                    Text(stringResource(Res.string.account_edit_profile), textAlign = TextAlign.Center)
                }
                OutlinedButton(onClick = { isPicking = true }, enabled = !state.isSaving, modifier = Modifier.weight(1f).heightIn(min = Sizes.touchTarget)) {
                    Text(stringResource(Res.string.account_change_avatar), textAlign = TextAlign.Center)
                }
            }
            if (state.canReview || state.canManageMembers) Tools(state, onReview, onValidate, onManageMembers)
            AppCard(Modifier.fillMaxWidth()) {
                MenuRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    text = stringResource(Res.string.session_sign_out),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onSignOut,
                )
            }
        }
    }
    if (isPicking) {
        AvatarPickerSheet(
            state = state,
            onDismiss = { isPicking = false },
            onPickPhoto = { isPicking = false; onPickPhoto() },
            onPreset = { isPicking = false; onEvent(AccountEvent.PresetChosen(it)) },
            onRemove = { isPicking = false; onEvent(AccountEvent.RemoveAvatarClicked) },
        )
    }
}

@Composable
private fun BigAvatar(state: AccountUiState, onClick: () -> Unit) {
    val label = stringResource(if (state.isSaving) Res.string.avatar_saving else Res.string.account_avatar_label)
    Box(
        Modifier
            .padding(top = Spacing.s)
            // Not clipped: the edit badge sits on the corner, outside the circle. The ripple stays round.
            .clickable(
                interactionSource = null,
                indication = ripple(bounded = false, radius = AVATAR_RIPPLE_RADIUS),
                enabled = !state.isSaving,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
    ) {
        Box(Modifier.border(BorderStroke(Sizes.strokeBold, BrandColors.gradient), CircleShape).padding(AVATAR_RING_GAP)) {
            MemberAvatar(state.me, size = Sizes.avatarL)
        }
        if (state.isSaving) {
            CircularProgressIndicator(Modifier.size(Sizes.avatarL + AVATAR_RING_GAP * 2).align(Alignment.Center), strokeWidth = Sizes.strokeBold)
        } else {
            Box(
                Modifier.align(Alignment.BottomEnd).size(EDIT_BADGE_SIZE).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background).padding(EDIT_BADGE_RING)
                    .clip(CircleShape).background(BrandColors.gradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = BrandColors.onGradient, modifier = Modifier.size(Sizes.iconXs))
            }
        }
    }
}

@Composable
private fun Identity(state: AccountUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            state.me.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        val instrument = state.instrument
        val level = state.level
        if (instrument != null && level != null) {
            val role = state.voicePart?.let { "${instrument.label()} ${it.label().lowercase()}" } ?: instrument.label()
            Text(
                stringResource(Res.string.account_role_line, role, level.label()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        state.role?.takeIf { it != MemberRole.MEMBER }?.let { role ->
            Text(
                role.label(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

@Composable
private fun Tools(state: AccountUiState, onReview: () -> Unit, onValidate: () -> Unit, onManageMembers: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            stringResource(Res.string.account_tools),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
        )
        AppCard(Modifier.fillMaxWidth()) {
            if (state.canReview) {
                MenuRow(Icons.Filled.Notifications, stringResource(Res.string.account_review), count = state.toReviewCount, onClick = onReview)
                MenuRow(Icons.Filled.CheckCircle, stringResource(Res.string.account_validate), count = state.toValidateCount, onClick = onValidate)
            }
            if (state.canManageMembers) {
                MenuRow(Icons.Filled.Person, stringResource(Res.string.account_members), onClick = onManageMembers)
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    count: Int = 0,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Sizes.actionHeight).clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(Spacing.m))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = tint, modifier = Modifier.weight(1f))
        if (count > 0) {
            Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("$count") }
            Spacer(Modifier.width(Spacing.s))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview
@Composable
private fun AccountScreenPreview() = AppTheme {
    AccountScreenContent(
        state = deriveAccountState(
            Member(
                "me",
                MemberProfile("Grâce Mbala", Instrument.SINGER, VoicePart.ALTO, Level.CONFIRMED),
                MemberRole.SUPER_ADMIN,
                Avatar.Preset(AvatarPreset.SINGER_GIRL_1),
            ),
        ).copy(toReviewCount = 3),
        onEvent = {}, onPickPhoto = {}, onBack = {}, onEditProfile = {}, onReview = {}, onValidate = {},
        onManageMembers = {}, onSignOut = {},
    )
}
