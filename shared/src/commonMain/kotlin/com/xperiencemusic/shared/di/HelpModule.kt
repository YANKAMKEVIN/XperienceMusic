package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.data.help.BundledHelpRepository
import com.xperiencemusic.shared.domain.repository.HelpRepository
import com.xperiencemusic.shared.domain.usecase.ObserveHelpUseCase
import com.xperiencemusic.shared.presentation.help.HelpViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The help pages ("Aide"), shipped with the app: the same in the demo and with Supabase. */
val helpModule = module {
    single<HelpRepository> { BundledHelpRepository() }
    factoryOf(::ObserveHelpUseCase)
    viewModelOf(::HelpViewModel)
}
