package com.xperiencemusic.shared.domain.repository

import kotlinx.datetime.LocalDate

fun interface DateProvider {
    fun today(): LocalDate
}
