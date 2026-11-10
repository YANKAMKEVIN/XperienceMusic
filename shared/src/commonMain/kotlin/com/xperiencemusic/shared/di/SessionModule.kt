package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.domain.usecase.ChangeAvatarUseCase
import com.xperiencemusic.shared.domain.usecase.CompleteOnboardingUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSessionUseCase
import com.xperiencemusic.shared.domain.usecase.RequestSignInCodeUseCase
import com.xperiencemusic.shared.domain.usecase.RetrySessionUseCase
import com.xperiencemusic.shared.domain.usecase.SignOutUseCase
import com.xperiencemusic.shared.domain.usecase.UpdateMyProfileUseCase
import com.xperiencemusic.shared.domain.usecase.VerifySignInCodeUseCase
import com.xperiencemusic.shared.presentation.account.AccountViewModel
import com.xperiencemusic.shared.presentation.app.AppViewModel
import com.xperiencemusic.shared.presentation.onboarding.OnboardingViewModel
import com.xperiencemusic.shared.presentation.profile.ProfileViewModel
import com.xperiencemusic.shared.presentation.signin.SignInViewModel
import com.xperiencemusic.shared.signInLinkInbox
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Signing in and out, the onboarding and the member's own account. */
val sessionModule = module {
    single { signInLinkInbox }
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::ObserveCurrentMemberUseCase)
    factoryOf(::RequestSignInCodeUseCase)
    factoryOf(::VerifySignInCodeUseCase)
    factoryOf(::RetrySessionUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::CompleteOnboardingUseCase)
    factoryOf(::UpdateMyProfileUseCase)
    factoryOf(::ChangeAvatarUseCase)
    viewModelOf(::AppViewModel)
    viewModelOf(::SignInViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::AccountViewModel)
}
