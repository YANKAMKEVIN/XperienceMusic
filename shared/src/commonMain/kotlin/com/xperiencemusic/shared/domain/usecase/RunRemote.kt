package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.ErrorLog
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainException
import com.xperiencemusic.shared.domain.model.DomainResult
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a repository call that may reach the network. Repositories signal domain failures with
 * [DomainException]; I/O failures (no network, timeout: the HTTP client reports them as
 * [IOException]s) are network problems; anything else is unexpected, logged and reported as
 * [DomainError.UNKNOWN].
 */
internal suspend fun <T> runRemote(block: suspend () -> T): DomainResult<T> =
    try {
        DomainResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: DomainException) {
        DomainResult.Failure(e.error)
    } catch (e: IOException) {
        DomainResult.Failure(DomainError.NETWORK)
    } catch (e: Exception) {
        ErrorLog.unexpected("Unexpected failure of a remote call", e)
        DomainResult.Failure(DomainError.UNKNOWN)
    }
