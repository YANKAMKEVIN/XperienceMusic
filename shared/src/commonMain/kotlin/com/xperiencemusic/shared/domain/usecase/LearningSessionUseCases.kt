package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.LearningPreferences
import com.xperiencemusic.shared.domain.model.LearningResume
import com.xperiencemusic.shared.domain.model.LearningSession
import com.xperiencemusic.shared.domain.repository.LearningSessionStore

class GetLearningPreferencesUseCase(private val store: LearningSessionStore) {
    operator fun invoke(memberId: String): LearningPreferences = store.preferences(memberId)
}

class SaveLearningPreferencesUseCase(private val store: LearningSessionStore) {
    operator fun invoke(memberId: String, preferences: LearningPreferences) = store.savePreferences(memberId, preferences)
}

/** Where the member left [moduleId] and what they set aside, as kept on the phone. */
class LoadLearningSessionUseCase(private val store: LearningSessionStore) {
    operator fun invoke(memberId: String, moduleId: String): LearningSession = store.session(memberId, moduleId)
}

class SaveLearningResumeUseCase(private val store: LearningSessionStore) {
    operator fun invoke(memberId: String, moduleId: String, resume: LearningResume) = store.saveResume(memberId, moduleId, resume)
}

class SavePostponedActivitiesUseCase(private val store: LearningSessionStore) {
    operator fun invoke(memberId: String, moduleId: String, postponed: Set<String>) =
        store.savePostponed(memberId, moduleId, postponed)
}
