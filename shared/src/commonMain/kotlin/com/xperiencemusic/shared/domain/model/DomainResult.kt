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
    ALREADY_INVITED,
    CANNOT_CHANGE_SELF,

    /** The server could not send the sign-in e-mail (its e-mail settings, not the phone's network). */
    CODE_NOT_SENT,
    TOO_MANY_ATTEMPTS,

    /** A link that is not the kind the field expects (a YouTube address, for a song). */
    INVALID_LINK,

    /** Neither a network problem nor a known domain failure (a bug, an unexpected server answer). */
    UNKNOWN,
}
