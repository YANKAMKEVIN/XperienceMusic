package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainException
import com.xperiencemusic.shared.domain.model.DomainResult
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a repository call that may reach the network. Repositories signal domain failures with
 * [DomainException]; anything else is treated as a network problem.
 */
internal suspend fun <T> runRemote(block: suspend () -> T): DomainResult<T> =
    try {
        DomainResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: DomainException) {
        DomainResult.Failure(e.error)
    } catch (e: Exception) {
        DomainResult.Failure(DomainError.NETWORK)
    }
