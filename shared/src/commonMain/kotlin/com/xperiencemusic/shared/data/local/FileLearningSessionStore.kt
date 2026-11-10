package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.mapper.toDto
import com.xperiencemusic.shared.domain.model.LearningPreferences
import com.xperiencemusic.shared.domain.model.LearningResume
import com.xperiencemusic.shared.domain.model.LearningSession
import com.xperiencemusic.shared.domain.repository.LearningSessionStore
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer

/** The guided path's settings and resume points, in the member's snapshots: signing out erases them. */
class FileLearningSessionStore(private val snapshots: SnapshotStore) : LearningSessionStore {
    override fun preferences(memberId: String): LearningPreferences =
        snapshots.load(preferencesKey(memberId), LearningPreferencesDto.serializer())?.toDomain() ?: LearningPreferences()

    override fun savePreferences(memberId: String, preferences: LearningPreferences) =
        snapshots.save(preferencesKey(memberId), LearningPreferencesDto.serializer(), preferences.toDto())

    override fun session(memberId: String, moduleId: String) = LearningSession(
        resume = snapshots.load(resumeKey(memberId, moduleId), LearningResumeDto.serializer())?.toDomain(),
        postponed = snapshots.load(postponedKey(memberId, moduleId), POSTPONED).orEmpty(),
    )

    override fun saveResume(memberId: String, moduleId: String, resume: LearningResume) =
        snapshots.save(resumeKey(memberId, moduleId), LearningResumeDto.serializer(), resume.toDto())

    override fun savePostponed(memberId: String, moduleId: String, postponed: Set<String>) =
        snapshots.save(postponedKey(memberId, moduleId), POSTPONED, postponed)

    private companion object {
        val POSTPONED = SetSerializer(String.serializer())

        fun preferencesKey(memberId: String) = "learning-settings-$memberId"

        fun resumeKey(memberId: String, moduleId: String) = "learning-resume-$memberId-$moduleId"

        fun postponedKey(memberId: String, moduleId: String) = "learning-postponed-$memberId-$moduleId"
    }
}
