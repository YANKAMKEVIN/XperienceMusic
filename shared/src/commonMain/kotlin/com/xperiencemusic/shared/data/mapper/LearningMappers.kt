package com.xperiencemusic.shared.data.mapper

import com.xperiencemusic.shared.data.local.LearningPreferencesDto
import com.xperiencemusic.shared.data.local.LearningResumeDto
import com.xperiencemusic.shared.data.local.StoredLearningAttemptDto
import com.xperiencemusic.shared.data.remote.dto.LearningAttemptDto
import com.xperiencemusic.shared.data.remote.dto.ObjectiveRecordingDto
import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.domain.model.LearningPreferences
import com.xperiencemusic.shared.domain.model.LearningResume
import com.xperiencemusic.shared.domain.model.ObjectiveRecording

fun LearningAttemptDto.toDomain() = LearningAttempt(id, memberId, activityId, version, passed, day, variant, hints)

fun LearningAttempt.toDto() = LearningAttemptDto(id, memberId, activityId, version, passed, day, variant, hints)

fun StoredLearningAttemptDto.toDomain() = LearningAttempt(id, memberId, activityId, version, passed, day, variant, hints)

fun LearningAttempt.toStored() = StoredLearningAttemptDto(id, memberId, activityId, version, passed, day, variant, hints)

fun ObjectiveRecordingDto.toDomain() = ObjectiveRecording(memberId, objectiveId, audioPath, seconds, submittedAt.orEmpty())

fun LearningPreferencesDto.toDomain() =
    LearningPreferences(touchSound, rewardSound, haptics, reduceMotion, largeText, weeklyTarget, startingPoint)

fun LearningPreferences.toDto() =
    LearningPreferencesDto(touchSound, rewardSound, haptics, reduceMotion, largeText, weeklyTarget, startingPoint)

fun LearningResumeDto.toDomain() = LearningResume(index, encounter, notes, answer, hints)

fun LearningResume.toDto() = LearningResumeDto(index, encounter, notes, answer, hints)
