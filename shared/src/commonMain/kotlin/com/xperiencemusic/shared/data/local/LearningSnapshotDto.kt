package com.xperiencemusic.shared.data.local

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/*
 * The guided path as kept on the phone. The field names are the ones files already written use:
 * renaming one would lose the members' history and settings.
 */

@Serializable
data class StoredAttemptDto(val attempt: StoredLearningAttemptDto, val synced: Boolean = false)

@Serializable
data class StoredLearningAttemptDto(
    val id: String,
    val memberId: String,
    val activityId: String,
    val version: Int,
    val passed: Boolean,
    val day: LocalDate,
    val variant: String = "original",
    val hints: Int = 0,
)

@Serializable
data class LearningPreferencesDto(
    val touchSound: Boolean = true,
    val rewardSound: Boolean = false,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val largeText: Boolean = false,
    val weeklyTarget: Int = 2,
    val startingPoint: String? = null,
)

@Serializable
data class LearningResumeDto(
    val index: Int = 0,
    val encounter: Int = 0,
    val notes: Set<Int> = emptySet(),
    val answer: String? = null,
    val hints: Int = 0,
)
