package com.xperiencemusic.shared.fakes

import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.domain.model.LearningPreferences
import com.xperiencemusic.shared.domain.model.LearningProgress
import com.xperiencemusic.shared.domain.model.LearningResume
import com.xperiencemusic.shared.domain.model.LearningSession
import com.xperiencemusic.shared.domain.model.ObjectiveRecording
import com.xperiencemusic.shared.domain.repository.LearningRepository
import com.xperiencemusic.shared.domain.repository.LearningSessionStore
import com.xperiencemusic.shared.domain.repository.ObjectiveRecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Attempts kept in memory; [failRecord] makes saving fail like a full disk. */
class FakeLearningRepository(attempts: List<LearningAttempt> = emptyList()) : LearningRepository {
    private val rows = MutableStateFlow(attempts)
    var failRecord = false
    var syncs = 0

    val attempts: List<LearningAttempt> get() = rows.value

    override fun observe(memberId: String): Flow<LearningProgress> =
        rows.map { all -> LearningProgress(all.filter { it.memberId == memberId }) }

    override suspend fun record(attempt: LearningAttempt) {
        if (failRecord) error("disk full")
        rows.value = rows.value + attempt
    }

    override suspend fun sync(memberId: String) {
        syncs++
    }
}

class FakeLearningSessionStore : LearningSessionStore {
    val preferences = mutableMapOf<String, LearningPreferences>()
    val resumes = mutableMapOf<Pair<String, String>, LearningResume>()
    val postponed = mutableMapOf<Pair<String, String>, Set<String>>()

    override fun preferences(memberId: String) = preferences[memberId] ?: LearningPreferences()

    override fun savePreferences(memberId: String, preferences: LearningPreferences) {
        this.preferences[memberId] = preferences
    }

    override fun session(memberId: String, moduleId: String) =
        LearningSession(resumes[memberId to moduleId], postponed[memberId to moduleId].orEmpty())

    override fun saveResume(memberId: String, moduleId: String, resume: LearningResume) {
        resumes[memberId to moduleId] = resume
    }

    override fun savePostponed(memberId: String, moduleId: String, postponed: Set<String>) {
        this.postponed[memberId to moduleId] = postponed
    }
}

/** Takes kept in memory; [failSend] makes the upload fail like a lost connection. */
class FakeObjectiveRecordingRepository(recordings: List<ObjectiveRecording> = emptyList()) : ObjectiveRecordingRepository {
    private val rows = MutableStateFlow(recordings)
    var failSend = false

    val sent: List<ObjectiveRecording> get() = rows.value

    override fun observe(memberId: String): Flow<List<ObjectiveRecording>> = rows.map { all -> all.filter { it.memberId == memberId } }

    override suspend fun send(memberId: String, objectiveId: String, wav: ByteArray, seconds: Int) {
        if (failSend) error("offline")
        rows.value = rows.value + ObjectiveRecording(memberId, objectiveId, "take-${rows.value.size}.wav", seconds, "2026-10-18T10:00:00Z")
    }
}
