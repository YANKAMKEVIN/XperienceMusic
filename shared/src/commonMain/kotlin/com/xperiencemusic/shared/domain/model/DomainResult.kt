package com.xperiencemusic.shared.domain.model

sealed interface DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>
}

enum class DomainError {
    FORBIDDEN,
    NOT_FOUND,
    INVALID_INPUT,
    RECORDING_LIMIT_REACHED,
    NOT_SIGNED_IN,
    INVALID_CODE,
    NETWORK,
    FILE_TOO_LARGE,
}
