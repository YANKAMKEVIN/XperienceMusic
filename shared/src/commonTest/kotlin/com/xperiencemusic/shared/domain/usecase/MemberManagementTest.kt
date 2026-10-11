package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemberManagementTest {
    private val admin = FakeSessionRepository(member(Role.SUPER_ADMIN))
    private val members = InMemoryMembersRepository(admin)

    @Test
    fun `seul un Admin++ peut inviter`() = runTest {
        val contentAdmin = FakeSessionRepository(member(Role.CONTENT_ADMIN))
        assertEquals(
            DomainResult.Failure(DomainError.FORBIDDEN),
            InviteMemberUseCase(contentAdmin, members)("new@band.fr", Role.MEMBER),
        )
    }

    @Test
    fun `une invitation est normalisée et apparaît en attente`() = runTest {
        assertEquals(DomainResult.Success("new@band.fr"), InviteMemberUseCase(admin, members)(" New@Band.FR ", Role.CONTENT_ADMIN))
        val band = ObserveBandUseCase(members)().first()
        assertTrue(band.pendingInvitations.any { it.email == "new@band.fr" && it.role == Role.CONTENT_ADMIN })
    }

    @Test
    fun `inviter deux fois la même adresse est refusé`() = runTest {
        assertEquals(
            DomainResult.Failure(DomainError.ALREADY_INVITED),
            InviteMemberUseCase(admin, members)("grace@exemple.fr", Role.MEMBER),
        )
    }

    @Test
    fun `les invitations déjà utilisées ne sont pas en attente`() = runTest {
        val band = ObserveBandUseCase(members)().first()
        assertEquals(listOf("ruth@exemple.fr"), band.pendingInvitations.map { it.email })
        // By name, members who have not onboarded yet last.
        assertEquals(listOf("demo-daniel", "me", "demo-grace", "demo-samuel"), band.members.map { it.id })
    }

    @Test
    fun `on ne peut ni changer ni retirer son propre accès`() = runTest {
        val me = ObserveBandUseCase(members)().first().members.first { it.id == "me" }
        assertEquals(DomainResult.Failure(DomainError.CANNOT_CHANGE_SELF), ChangeMemberRoleUseCase(admin, members)(me, Role.MEMBER))
        assertEquals(DomainResult.Failure(DomainError.CANNOT_CHANGE_SELF), RemoveMemberUseCase(admin, members)(me))
    }

    @Test
    fun `changer un rôle et retirer un membre`() = runTest {
        val grace = ObserveBandUseCase(members)().first().members.first { it.id == "demo-grace" }
        ChangeMemberRoleUseCase(admin, members)(grace, Role.CONTENT_ADMIN)
        assertEquals(Role.CONTENT_ADMIN, members.observeMembers().first().first { it.id == grace.id }.role)
        RemoveMemberUseCase(admin, members)(grace)
        assertTrue(members.observeMembers().first().none { it.id == grace.id })
        assertTrue(members.observeInvitations().first().none { it.email == grace.email })
    }
}
