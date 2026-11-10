package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.LearningPreferences
import com.xperiencemusic.shared.domain.model.LearningResume
import com.xperiencemusic.shared.domain.model.LearningSession

/**
 * What the phone keeps of the guided path for each member: the comfort settings, and for each
 * module where they left off and the sung activities set aside.
 */
interface LearningSessionStore {
    fun preferences(memberId: String): LearningPreferences

    fun savePreferences(memberId: String, preferences: LearningPreferences)

    fun session(memberId: String, moduleId: String): LearningSession

    fun saveResume(memberId: String, moduleId: String, resume: LearningResume)

    fun savePostponed(memberId: String, moduleId: String, postponed: Set<String>)
}
