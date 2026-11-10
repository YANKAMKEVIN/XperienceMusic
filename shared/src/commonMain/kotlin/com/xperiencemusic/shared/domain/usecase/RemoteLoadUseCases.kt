package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.repository.RemoteLoadStatus
import kotlinx.coroutines.flow.Flow

/** Whether some data could not be fetched, so a screen may be incomplete or out of date. */
class ObserveRemoteLoadFailureUseCase(private val status: RemoteLoadStatus) {
    operator fun invoke(): Flow<Boolean> = status.hasFailure
}

/** Tries the failed loads again. */
class RetryRemoteLoadsUseCase(private val status: RemoteLoadStatus) {
    suspend operator fun invoke() = status.retry()
}
