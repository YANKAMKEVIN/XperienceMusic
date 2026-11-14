package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.data.repository.InMemoryGrowthRepository
import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.data.seed.GrowthObjectives
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.learning.PianoBasics
import com.xperiencemusic.shared.domain.model.ActivityAnswer
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.GradeResult
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.domain.model.LearningPreferences
import com.xperiencemusic.shared.domain.model.LearningResume
import com.xperiencemusic.shared.domain.model.ObjectiveCheck
import com.xperiencemusic.shared.domain.model.ObjectiveRecording
import com.xperiencemusic.shared.domain.model.SubmissionOutcome
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.fakes.FakeLearningRepository
import com.xperiencemusic.shared.fakes.FakeLearningSessionStore
import com.xperiencemusic.shared.fakes.FakeObjectiveRecordingRepository
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LearningUseCasesTest {
    private val today = LocalDate(2026, 10, 18)
    private val dates = DateProvider { today }
    private val chords = LearningCatalog.module(LearningCatalog.DEFAULT)
    private val cMajor = PianoBasics.activities.first { it.id == "piano-chords-build-major" }

    private fun finished(module: com.xperiencemusic.shared.domain.model.LearningModule) =
        module.activities.map { LearningAttempt(it.id, "me", it.id, PianoBasics.VERSION, true, today) }

    @Test
    fun anAttemptIsCountedAgainstTheCurrentVersionWithItsVariant() = runTest {
        val repository = FakeLearningRepository()
        val result = RecordLearningAttemptUseCase(repository, dates)("me", cMajor, encounter = 2, passed = true, hints = 1)
        assertIs<DomainResult.Success<Unit>>(result)
        val attempt = repository.attempts.single()
        assertEquals(PianoBasics.VERSION, attempt.version)
        assertEquals("${cMajor.id}:2", attempt.variant)
        assertEquals(today, attempt.day)
        assertEquals(1, attempt.hints)
    }

    @Test
    fun theVerdictComesOnlyOnceTheAttemptIsSaved() = runTest {
        val repository = FakeLearningRepository()
        val grade = GradeActivityUseCase(RecordLearningAttemptUseCase(repository, dates))
        val wrong = grade("me", cMajor, 0, ActivityAnswer(notes = setOf(60, 64)), hints = 0)
        assertIs<GradeResult.WrongNotes>(assertIs<DomainResult.Success<GradeResult>>(wrong).value)
        assertEquals(false, repository.attempts.single().passed)

        repository.failRecord = true
        val failed = grade("me", cMajor, 0, ActivityAnswer(notes = setOf(60, 64, 67)), hints = 0)
        assertIs<DomainResult.Failure>(failed)
        assertEquals(1, repository.attempts.size)
    }

    @Test
    fun theMemberSeesOnlyTheirOwnChecks() = runTest {
        val sessions = FakeSessionRepository(member(instrument = Instrument.PIANO))
        val growth = InMemoryGrowthRepository(
            sessions, InMemoryMembersRepository(sessions), dates,
            checks = listOf(
                ObjectiveCheck("me", "Grâce", Instrument.PIANO, "piano-c2", ExerciseStatus.SUBMITTED, null, today),
                ObjectiveCheck("other", "Paul", Instrument.PIANO, "piano-c2", ExerciseStatus.VALIDATED, null, today),
            ),
        )
        assertEquals(listOf("me"), ObserveObjectiveChecksUseCase(growth)("me").first().map { it.memberId })
    }

    @Test
    fun takesComeNewestFirst() = runTest {
        val recordings = FakeObjectiveRecordingRepository(
            listOf(
                ObjectiveRecording("me", "piano-c2", "old.wav", 4, "2026-10-01T10:00:00Z"),
                ObjectiveRecording("me", "piano-c2", "new.wav", 5, "2026-10-12T10:00:00Z"),
                ObjectiveRecording("other", "piano-c2", "theirs.wav", 5, "2026-10-13T10:00:00Z"),
            ),
        )
        assertEquals(listOf("new.wav", "old.wav"), ObserveObjectiveRecordingsUseCase(recordings)("me").first().map { it.audioPath })
    }

    private class Submission(instrument: Instrument = Instrument.PIANO, checks: List<ObjectiveCheck> = emptyList()) {
        val sessions = FakeSessionRepository(member(instrument = instrument))
        val learning = FakeLearningRepository()
        val recordings = FakeObjectiveRecordingRepository()
        val growth = InMemoryGrowthRepository(sessions, InMemoryMembersRepository(sessions), { LocalDate(2026, 10, 18) }, checks = checks)
        val submit = SubmitObjectiveRecordingUseCase(sessions, learning, recordings, TickObjectiveUseCase(sessions, growth, GrowthObjectives.all))
    }

    @Test
    fun aTakeIsSentWithItsEvaluationRequestOnceTheModuleIsFinished() = runTest {
        val s = Submission()
        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), s.submit(chords, byteArrayOf(1), 5), "module not finished")
        finished(chords).forEach { s.learning.record(it) }
        assertEquals(DomainResult.Success(SubmissionOutcome.REVIEW_REQUESTED), s.submit(chords, byteArrayOf(1), 5))
        assertEquals("piano-c2", s.recordings.sent.single().objectiveId)
        assertEquals(ExerciseStatus.SUBMITTED, s.growth.observeChecks().first().single().status)
    }

    @Test
    fun aTakeIsKeptWhenTheRequestCannotFollow() = runTest {
        val validated = ObjectiveCheck("me", "Grâce", Instrument.PIANO, "piano-c2", ExerciseStatus.VALIDATED, null, today)
        val s = Submission(checks = listOf(validated))
        finished(chords).forEach { s.learning.record(it) }
        assertEquals(DomainResult.Success(SubmissionOutcome.TAKE_KEPT), s.submit(chords, byteArrayOf(1), 5))
        assertEquals(1, s.recordings.sent.size)
    }

    @Test
    fun anotherPartOrALostConnectionSendsNothing() = runTest {
        val singer = Submission(instrument = Instrument.SINGER)
        finished(chords).forEach { singer.learning.record(it) }
        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), singer.submit(chords, byteArrayOf(1), 5))

        val offline = Submission()
        finished(chords).forEach { offline.learning.record(it) }
        offline.recordings.failSend = true
        assertEquals(DomainResult.Failure(DomainError.NETWORK), offline.submit(chords, byteArrayOf(1), 5))
        assertTrue(offline.growth.observeChecks().first().isEmpty())
    }

    @Test
    fun theSessionUseCasesReadBackWhatTheySaved() {
        val store = FakeLearningSessionStore()
        SaveLearningPreferencesUseCase(store)("me", LearningPreferences(reduceMotion = true))
        SaveLearningResumeUseCase(store)("me", "piano-chords", LearningResume(index = 3))
        SavePostponedActivitiesUseCase(store)("me", "singer-hold", setOf("a"))
        assertTrue(GetLearningPreferencesUseCase(store)("me").reduceMotion)
        assertEquals(3, LoadLearningSessionUseCase(store)("me", "piano-chords").resume?.index)
        assertEquals(setOf("a"), LoadLearningSessionUseCase(store)("me", "singer-hold").postponed)
    }
}
