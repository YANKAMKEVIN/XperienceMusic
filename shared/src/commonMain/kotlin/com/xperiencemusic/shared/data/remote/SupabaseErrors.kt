package com.xperiencemusic.shared.data.remote

import com.xperiencemusic.shared.domain.ErrorLog
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException

/**
 * The database functions raise errors named after [DomainError] ("FORBIDDEN",
 * "RECORDING_LIMIT_REACHED"…); this turns them back into [DomainException]s.
 */
internal suspend fun <T> postgrestCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: PostgrestRestException) {
        val error = e.toDomainError()
        if (error == DomainError.UNKNOWN) ErrorLog.unexpected("Unexpected database error ${e.code}: ${e.error}", e)
        throw DomainException(error)
    }

internal fun PostgrestRestException.toDomainError(): DomainError =
    DomainError.entries.firstOrNull { it.name == error }
        ?: when (code) {
            "42501" -> DomainError.FORBIDDEN
            "P0002" -> DomainError.NOT_FOUND
            // The server answered: whatever went wrong, it was not the network.
            else -> DomainError.UNKNOWN
        }
