package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.LocalDateTime

/** A notification the phone shows at [at], local time; one already due is shown straight away. */
data class Reminder(
    val id: String,
    val at: LocalDateTime,
    val title: String,
    val body: String,
)
