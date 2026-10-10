package com.xperiencemusic.shared.domain.model

/** Lets repositories report a business failure (e.g. the server refused) through suspend calls. */
class DomainException(val error: DomainError) : Exception(error.name)
