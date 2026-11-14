package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.data.repository.InMemoryGrowthRepository
import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.data.repository.InMemoryVoiceSubmissionRepository
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.LevelRequest
import com.xperiencemusic.shared.domain.model.ObjectiveCheck
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewQueueUseCasesTest {
    private val day = LocalDate(2026, 11, 2)
    private val today = DateProvider { day }

    private fun voice(id: String, memberId: String, songId: String, status: ExerciseStatus) =
        VoiceSubmission(id, memberId, "Grâce", null, songId, "Chant $songId", "demo://$id", 180, day, status, null)

    private val voices = listOf(
        voice("v1", "me", "s1", ExerciseStatus.SUBMITTED),
        voice("v2", "me", "s2", ExerciseStatus.VALIDATED),
        voice("v3", "other", "s1", ExerciseStatus.SUBMITTED),
    )
    private val requests = listOf(
        LevelRequest("r1", "other", "Paul", Instrument.DRUMS, null, Level.CONFIRMED, ExerciseStatus.SUBMITTED, null, day),
        LevelRequest("r2", "other", "Paul", Instrument.DRUMS, null, Level.CONFIRMED, ExerciseStatus.VALIDATED, null, day),
    )
    private val checks = listOf(
        ObjectiveCheck("other", "Paul", Instrument.DRUMS, "o1", ExerciseStatus.SUBMITTED, null, day),
        ObjectiveCheck("other", "Paul", Instrument.DRUMS, "o2", ExerciseStatus.SUBMITTED, null, day),
        ObjectiveCheck("other", "Paul", Instrument.DRUMS, "o3", ExerciseStatus.TO_REWORK, "Encore", day),
    )

    private inner class World(role: Role) {
        val sessions = FakeSessionRepository(member(role))
        val currentMember = ObserveCurrentMemberUseCase(sessions)
        val submissions = InMemoryVoiceSubmissionRepository(sessions, songRepository(), today, voices)
        val growth = InMemoryGrowthRepository(sessions, InMemoryMembersRepository(sessions), today, requests, checks)
    }

    @Test
    fun `le relecteur voit ce qui attend encore une réponse`() = runTest {
        val world = World(Role.SUPER_ADMIN)
        val queue = ObserveReviewQueueUseCase(world.currentMember, world.submissions, world.growth)().first()

        assertEquals(listOf("v1", "v3"), queue.voices.map { it.id })
        assertEquals(listOf("r1"), queue.levels.map { it.id })
        assertEquals(listOf("o1", "o2"), queue.objectives.map { it.objectiveId })
        assertEquals(2, queue.toReview)
        assertEquals(3, queue.toValidate)
    }

    @Test
    fun `sans la permission de relire la file reste vide`() = runTest {
        val world = World(Role.MEMBER)

        assertEquals(ReviewQueue(), ObserveReviewQueueUseCase(world.currentMember, world.submissions, world.growth)().first())
        assertTrue(ObserveBandSubmissionsUseCase(world.currentMember, world.submissions)().first().isEmpty())
    }

    @Test
    fun `le relecteur entend tout le groupe, réponses données comprises`() = runTest {
        val world = World(Role.SUPER_ADMIN)

        assertEquals(listOf("v1", "v2", "v3"), ObserveBandSubmissionsUseCase(world.currentMember, world.submissions)().first().map { it.id }.sorted())
    }

    @Test
    fun `chacun retrouve ses envois, tous ou celui d'un chant`() = runTest {
        val world = World(Role.MEMBER)
        val mine = ObserveMySubmissionsUseCase(world.currentMember, world.submissions)

        assertEquals(listOf("v1", "v2"), mine().first().map { it.id }.sorted())
        assertEquals(listOf("v1"), mine("s1").first().map { it.id })

        world.sessions.state.value = SessionState.SignedOut
        assertTrue(mine().first().isEmpty())
    }
}
