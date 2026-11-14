package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.domain.repository.SessionResettable
import org.koin.core.definition.KoinDefinition
import org.koin.dsl.bind

/**
 * Also registers this definition as a [SessionResettable], so that signing out resets it (see
 * `DeviceDataStore`). Only compiles for a type that implements the interface.
 */
fun <T : SessionResettable> KoinDefinition<T>.resetAtSignOut(): KoinDefinition<T> = apply {
    bind(SessionResettable::class)
}
