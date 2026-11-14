package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.domain.usecase.CancelInvitationUseCase
import com.xperiencemusic.shared.domain.usecase.ChangeMemberRoleUseCase
import com.xperiencemusic.shared.domain.usecase.InviteMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveBandUseCase
import com.xperiencemusic.shared.domain.usecase.RemoveMemberUseCase
import com.xperiencemusic.shared.presentation.members.MembersViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The band: members, roles and invitations. */
val membersModule = module {
    factoryOf(::ObserveBandUseCase)
    factoryOf(::InviteMemberUseCase)
    factoryOf(::ChangeMemberRoleUseCase)
    factoryOf(::RemoveMemberUseCase)
    factoryOf(::CancelInvitationUseCase)
    viewModelOf(::MembersViewModel)
}
