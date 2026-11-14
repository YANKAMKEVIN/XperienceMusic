package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.LocalDate

/** Versioned activities train skills; only a responsible person validates the objective. */
enum class LearningKind { LESSON, EAR, KEYBOARD, PRACTICE, CHOICE, RHYTHM, SING }

/**
 * What a SING activity asks: the notes to sing, in semitones from the singer's comfort note (so each
 * voice gets its own), each held [secondsEach]. [accompanimentSemitones] plays another voice's note
 * meanwhile, to hold one's part against it.
 */
data class SingTarget(
    val semitones: List<Int>,
    val secondsEach: Double,
    val accompanimentSemitones: Int? = null,
)

data class LearningActivity(
    val id: String,
    val title: String,
    val kind: LearningKind,
    val instruction: String,
    val explanation: String,
    val example: List<Int> = emptyList(),
    val expectedNotes: Set<Int> = emptySet(),
    val answer: String? = null,
    val choices: List<String> = emptyList(),
    val bpm: Int = 72,
    val sing: SingTarget? = null,
)

data class LearningModule(
    val id: String,
    val title: String,
    val objectiveId: String,
    val activities: List<LearningActivity>,
    val instrument: Instrument = Instrument.PIANO,
)

data class LearningAttempt(
    val id: String,
    val memberId: String,
    val activityId: String,
    val version: Int,
    val passed: Boolean,
    val day: LocalDate,
    val variant: String = "original",
    val hints: Int = 0,
)

/** A challenge to prepare before sending a take to the responsable. */
data class MusicalChallenge(
    val title: String,
    val steps: List<String>,
    val criteria: String,
    val example: List<Int>,
)

/** A take sent for an objective, kept so the member and the reviewer can hear progress over time. */
data class ObjectiveRecording(
    val memberId: String,
    val objectiveId: String,
    val audioPath: String,
    val seconds: Int,
    val submittedAt: String = "",
)

/** The comfort settings of the guided path, kept on the phone for each member. */
data class LearningPreferences(
    val touchSound: Boolean = true,
    val rewardSound: Boolean = false,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val largeText: Boolean = false,
    val weeklyTarget: Int = 2,
    val startingPoint: String? = null,
)

/** Where the member left an activity of a module, with what they had selected so far. */
data class LearningResume(
    val index: Int = 0,
    val encounter: Int = 0,
    val notes: Set<Int> = emptySet(),
    val answer: String? = null,
    val hints: Int = 0,
)

/** What the phone remembers of a module between visits. */
data class LearningSession(
    val resume: LearningResume? = null,
    /** Sung activities set aside for later. */
    val postponed: Set<String> = emptySet(),
)
