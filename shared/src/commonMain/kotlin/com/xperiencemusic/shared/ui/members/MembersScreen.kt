package com.xperiencemusic.shared.ui.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.members.InvitationRowUi
import com.xperiencemusic.shared.presentation.members.MemberRowUi
import com.xperiencemusic.shared.presentation.members.MembersEvent
import com.xperiencemusic.shared.presentation.members.MembersMessage
import com.xperiencemusic.shared.presentation.members.MembersUiState
import com.xperiencemusic.shared.presentation.members.MembersViewModel
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.action_back
import com.xperiencemusic.shared.resources.error_forbidden
import com.xperiencemusic.shared.resources.error_generic
import com.xperiencemusic.shared.resources.members_cancel_invitation
import com.xperiencemusic.shared.resources.members_change_role
import com.xperiencemusic.shared.resources.members_invite_button
import com.xperiencemusic.shared.resources.members_invite_email
import com.xperiencemusic.shared.resources.members_invite_help
import com.xperiencemusic.shared.resources.members_invite_title
import com.xperiencemusic.shared.resources.members_list_title
import com.xperiencemusic.shared.resources.members_me
import com.xperiencemusic.shared.resources.members_msg_already
import com.xperiencemusic.shared.resources.members_msg_invalid
import com.xperiencemusic.shared.resources.members_msg_invited
import com.xperiencemusic.shared.resources.members_msg_self
import com.xperiencemusic.shared.resources.members_not_onboarded
import com.xperiencemusic.shared.resources.members_pending_help
import com.xperiencemusic.shared.resources.members_pending_title
import com.xperiencemusic.shared.resources.members_remove
import com.xperiencemusic.shared.resources.members_remove_ok
import com.xperiencemusic.shared.resources.members_remove_text
import com.xperiencemusic.shared.resources.members_remove_title
import com.xperiencemusic.shared.resources.members_title
import com.xperiencemusic.shared.resources.song_delete_confirm_cancel
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.components.ChoiceChips
import com.xperiencemusic.shared.ui.components.SectionTitle
import com.xperiencemusic.shared.ui.components.label
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MembersScreen(onBack: () -> Unit, viewModel: MembersViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MembersScreenContent(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreenContent(state: MembersUiState, onEvent: (MembersEvent) -> Unit, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(
            getString(
                when (message) {
                    MembersMessage.INVITED -> Res.string.members_msg_invited
                    MembersMessage.ALREADY_INVITED -> Res.string.members_msg_already
                    MembersMessage.INVALID_EMAIL -> Res.string.members_msg_invalid
                    MembersMessage.CANNOT_CHANGE_SELF -> Res.string.members_msg_self
                    MembersMessage.FORBIDDEN -> Res.string.error_forbidden
                    MembersMessage.NETWORK -> Res.string.error_generic
                },
            ),
        )
        onEvent(MembersEvent.MessageShown)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.members_title)) },
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
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).imePadding(),
            contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            item { InviteCard(state, onEvent) }
            item { SectionTitle(stringResource(Res.string.members_list_title, state.members.size), Modifier.padding(top = Spacing.m)) }
            items(state.members, key = { it.id }) { MemberRow(it, onEvent) }
            if (state.pendingInvitations.isNotEmpty()) {
                item {
                    SectionTitle(
                        stringResource(Res.string.members_pending_title, state.pendingInvitations.size),
                        Modifier.padding(top = Spacing.m),
                    )
                    Text(
                        stringResource(Res.string.members_pending_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.pendingInvitations, key = { it.email }) { InvitationRow(it, onEvent) }
            }
        }
    }

    state.pendingRemoval?.let { member ->
        AlertDialog(
            onDismissRequest = { onEvent(MembersEvent.RemoveDismissed) },
            title = { Text(stringResource(Res.string.members_remove_title, member.name)) },
            text = { Text(stringResource(Res.string.members_remove_text)) },
            confirmButton = {
                TextButton(onClick = { onEvent(MembersEvent.RemoveConfirmed) }) {
                    Text(stringResource(Res.string.members_remove_ok), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(MembersEvent.RemoveDismissed) }) {
                    Text(stringResource(Res.string.song_delete_confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun InviteCard(state: MembersUiState, onEvent: (MembersEvent) -> Unit) {
    AppCard {
        SectionTitle(stringResource(Res.string.members_invite_title))
        Text(
            stringResource(Res.string.members_invite_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = Spacing.s),
        )
        OutlinedTextField(
            value = state.inviteEmail,
            onValueChange = { onEvent(MembersEvent.InviteEmailChanged(it)) },
            label = { Text(stringResource(Res.string.members_invite_email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        ChoiceChips(
            options = Role.entries,
            selected = state.inviteRole,
            label = { it.label() },
            onSelect = { onEvent(MembersEvent.InviteRoleSelected(it)) },
            modifier = Modifier.padding(top = Spacing.s),
        )
        Button(onClick = { onEvent(MembersEvent.InviteClicked) }, enabled = state.canInvite, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.members_invite_button))
        }
    }
}

@Composable
private fun MemberRow(member: MemberRowUi, onEvent: (MembersEvent) -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (member.isMe) "${member.name} (${stringResource(Res.string.members_me)})" else member.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    when {
                        !member.hasOnboarded -> stringResource(Res.string.members_not_onboarded)
                        member.voicePart != null -> "${member.instrument?.label().orEmpty()} · ${member.voicePart.label()}"
                        else -> member.instrument?.label().orEmpty()
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RoleMenu(member, onEvent)
            if (!member.isMe) {
                IconButton(onClick = { onEvent(MembersEvent.RemoveClicked(member.id)) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.members_remove, member.name))
                }
            }
        }
    }
}

@Composable
private fun RoleMenu(member: MemberRowUi, onEvent: (MembersEvent) -> Unit) {
    var isOpen by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { isOpen = true }, enabled = !member.isMe) {
            Text(member.role.label())
            if (!member.isMe) {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(Res.string.members_change_role, member.name))
            }
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            Role.entries.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role.label()) },
                    onClick = {
                        isOpen = false
                        if (role != member.role) onEvent(MembersEvent.RoleChanged(member.id, role))
                    },
                )
            }
        }
    }
}

@Composable
private fun InvitationRow(invitation: InvitationRowUi, onEvent: (MembersEvent) -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(invitation.email, style = MaterialTheme.typography.titleSmall)
                Text(invitation.role.label(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = { onEvent(MembersEvent.CancelInvitationClicked(invitation.email)) }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.members_cancel_invitation, invitation.email))
            }
        }
    }
}

@Preview
@Composable
private fun MembersScreenPreview() {
    AppTheme {
        MembersScreenContent(
            MembersUiState(
                isLoading = false,
                members = listOf(
                    MemberRowUi("1", "Kevin", "k@x.fr", true, Role.SUPER_ADMIN, Instrument.SINGER, VoicePart.TENOR, isMe = true),
                    MemberRowUi("2", "Grâce", "g@x.fr", true, Role.MEMBER, Instrument.SINGER, VoicePart.ALTO, isMe = false),
                    MemberRowUi("3", "samuel@x.fr", "samuel@x.fr", false, Role.MEMBER, null, null, isMe = false),
                ),
                pendingInvitations = listOf(InvitationRowUi("ruth@x.fr", Role.MEMBER)),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
