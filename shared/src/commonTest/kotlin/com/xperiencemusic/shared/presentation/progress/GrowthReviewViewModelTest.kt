package com.xperiencemusic.shared.presentation.progress

import app.cash.turbine.test
import com.xperiencemusic.shared.data.repository.InMemoryAudioStorage
import com.xperiencemusic.shared.data.repository.InMemoryGrowthRepository
import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.data.repository.InMemoryObjectiveRecordingRepository
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.LevelRequest
import com.xperiencemusic.shared.domain.model.Objective
import com.xperiencemusic.shared.domain.model.ObjectiveCheck
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.usecase.ObserveGrowthReviewUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveObjectiveRecordingsUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.ReviewLevelRequestUseCase
import com.xperiencemusic.shared.domain.usecase.ReviewObjectiveUseCase
import com.xperiencemusic.shared.fakes.FakeAudioPlayer
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GrowthReviewViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val day = LocalDate(2026, 10, 16)

    @Test
    fun `l'Admin++ confirme un passage et renvoie un objectif avec un commentaire`() = runTest {
        val sessions = FakeSessionRepository(member(Role.SUPER_ADMIN))
        val growth = InMemoryGrowthRepository(
            sessions, InMemoryMembersRepository(sessions), { day },
            requests = listOf(LevelRequest("r1", "g", "Grâce", Instrument.SINGER, null, Level.CONFIRMED, ExerciseStatus.SUBMITTED, null, day)),
            checks = listOf(ObjectiveCheck("d", "Daniel", Instrument.PIANO, "piano-c1", ExerciseStatus.SUBMITTED, null, day)),
        )
        val viewModel = GrowthReviewViewModel(
            ObserveGrowthReviewUseCase(growth),
            listOf(Objective("piano-c1", Instrument.PIANO, Level.CONFIRMED, 1, "Les accords", "Triades", "I-IV-V-I")),
            ReviewLevelRequestUseCase(sessions, growth),
            ReviewObjectiveUseCase(sessions, growth),
            ObserveObjectiveRecordingsUseCase(InMemoryObjectiveRecordingRepository(InMemoryAudioStorage())),
            PlaybackController(FakeAudioPlayer(), { it }, backgroundScope),
        )
        viewModel.state.test {
            val first = expectMostRecentItem()
            assertEquals("Les accords", first.objectives.single().title)
            viewModel.onEvent(GrowthReviewEvent.Answered(first.requests.single().key, validated = true))

            val objectiveKey = first.objectives.single().key
            viewModel.onEvent(GrowthReviewEvent.Answered(objectiveKey, validated = false))
            assertEquals(objectiveKey, expectMostRecentItem().missingCommentKey, "a send-back needs a comment")
            viewModel.onEvent(GrowthReviewEvent.CommentChanged(objectiveKey, "Encore une hésitation au refrain"))
            viewModel.onEvent(GrowthReviewEvent.Answered(objectiveKey, validated = false))

            val last = expectMostRecentItem()
            assertTrue(last.requests.isEmpty() && last.objectives.isEmpty(), "answered items leave the queue")
        }
        assertEquals(ExerciseStatus.VALIDATED, growth.observeRequests().first().single().status)
        assertEquals("Encore une hésitation au refrain", growth.observeChecks().first().single().feedback)
    }

    @Test
    fun `les prises envoyées pour un objectif s'écoutent depuis la carte`() = runTest {
        val sessions = FakeSessionRepository(member(Role.SUPER_ADMIN))
        val growth = InMemoryGrowthRepository(
            sessions, InMemoryMembersRepository(sessions), { day },
            checks = listOf(ObjectiveCheck("d", "Daniel", Instrument.PIANO, "piano-c1", ExerciseStatus.SUBMITTED, null, day)),
        )
        val recordings = InMemoryObjectiveRecordingRepository(InMemoryAudioStorage())
        recordings.send("d", "piano-c1", byteArrayOf(1), 4)
        recordings.send("d", "piano-c1", byteArrayOf(2), 6)
        recordings.send("d", "piano-c2", byteArrayOf(3), 5)
        val player = FakeAudioPlayer()
        val playback = PlaybackController(player, { it }, backgroundScope)
        val viewModel = GrowthReviewViewModel(
            ObserveGrowthReviewUseCase(growth),
            listOf(Objective("piano-c1", Instrument.PIANO, Level.CONFIRMED, 1, "Les accords", "Triades", "I-IV-V-I")),
            ReviewLevelRequestUseCase(sessions, growth),
            ReviewObjectiveUseCase(sessions, growth),
            ObserveObjectiveRecordingsUseCase(recordings),
            playback,
        )
        viewModel.state.test {
            val takes = expectMostRecentItem().objectives.single().takes
            assertEquals(listOf(2, 1), takes.map { it.number }, "only this objective's takes, newest first")
            viewModel.onEvent(GrowthReviewEvent.PlayTake(takes.first(), "Prise", "6 secondes"))
            runCurrent()
            assertTrue(expectMostRecentItem().objectives.single().takes.first().isPlaying)
        }
    }
}
