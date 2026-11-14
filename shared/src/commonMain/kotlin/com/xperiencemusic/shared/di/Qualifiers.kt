package com.xperiencemusic.shared.di

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named

/** The coroutine scopes that live as long as the app. */
object AppScopes {
    /** Background work that outlives every screen, such as following the auth session. */
    val Background: Qualifier = named("appScope")

    /** Work bound to the main thread, which the platform players expect. */
    val Main: Qualifier = named("mainScope")
}
