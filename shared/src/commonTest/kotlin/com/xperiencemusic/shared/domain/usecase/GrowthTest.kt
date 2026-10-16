package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.data.repository.InMemoryExerciseRepository
import com.xperiencemusic.shared.data.repository.InMemoryGrowthRepository
import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.ExerciseCheck
import com.xperiencemusic.shared.domain.model.ExerciseProgress
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.Objective
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.SingerPath
import com.xperiencemusic.shared.domain.model.levelProgress
import com.xperiencemusic.shared.domain.model.next
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GrowthTest {
    private val today = LocalDate(2026, 10, 16)
    private val objectives = listOf(
        Objective("piano-c1", Instrument.PIANO, Level.CONFIRMED, 1, "Accords", ""),
        Objective("piano-c2", Instrument.PIANO, Level.CONFIRMED, 2, "Tempo", ""),
        Objective("piano-p1", Instrument.PIANO, Level.PRO, 1, "Sans grille", ""),
        Objective("bass-c1", Instrument.BASS, Level.CONFIRMED, 1, "Fondamentales", ""),
    )

    /** One exercise per tier, the first [done] tiers validated. */
    private fun path(done: Int) = SingerPath.of(
        (1..5).map { tier ->
            ExerciseProgress(
                Exercise("t$tier", tier, 1, "Palier $tier", "", ExerciseCheck.HoldNote(0, 5.0)),
                if (tier <= done) ExerciseStatus.VALIDATED else ExerciseStatus.TODO,
            )
        },
        warmUp = null,
        streakDays = 0,
    )

    @Test
    fun `les niveaux vont de Débutant à Pro`() {
        assertEquals(Level.CONFIRMED, Level.BEGINNER.next())
        assertEquals(Level.PRO, Level.CONFIRMED.next())
        assertNull(Level.PRO.next())
    }

    @Test
    fun `un chantre devient Confirmé avec trois paliers et Pro avec les cinq`() {
        val beginner = levelProgress(member(level = Level.BEGINNER), path(2), objectives, emptyList(), emptyList())
        assertEquals(2 to 3, beginner.done to beginner.required)
        assertFalse(beginner.canAsk)
        assertTrue(levelProgress(member(level = Level.BEGINNER), path(3), objectives, emptyList(), emptyList()).canAsk)

        val confirmed = levelProgress(member(level = Level.CONFIRMED), path(4), objectives, emptyList(), emptyList())
        assertEquals(Level.PRO, confirmed.next)
        assertEquals(4 to 5, confirmed.done to confirmed.required)

        val pro = levelProgress(member(level = Level.PRO), path(5), objectives, emptyList(), emptyList())
        assertNull(pro.next)
        assertFalse(pro.canAsk)
    }

    @Test
    fun `un musicien avance avec ses objectifs validés seulement`() = runTest {
        val pianist = member(instrument = Instrument.PIANO, voicePart = null)
        val sessions = FakeSessionRepository(pianist)
        val growth = InMemoryGrowthRepository(sessions, InMemoryMembersRepository(sessions), { today })
        val tick = TickObjectiveUseCase(sessions, growth, objectives)

        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), tick("bass-c1", true), "not their instrument")
        tick("piano-c1", true)
        tick("piano-c2", true)
        val ticked = levelProgress(pianist, null, objectives, growth.observeChecks().first(), emptyList())
        assertEquals(0 to 2, ticked.done to ticked.required, "ticked is not validated")

        growth.reviewObjective("me", "piano-c1", ExerciseStatus.VALIDATED, null)
        growth.reviewObjective("me", "piano-c2", ExerciseStatus.VALIDATED, null)
        assertTrue(levelProgress(pianist, null, objectives, growth.observeChecks().first(), emptyList()).canAsk)
        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), tick("piano-c1", false), "a validated objective stays")
    }

    @Test
    fun `la demande attend que tout soit fait, puis seul l'Admin++ y répond`() = runTest {
        val singer = member(level = Level.BEGINNER)
        val sessions = FakeSessionRepository(singer)
        val growth = InMemoryGrowthRepository(sessions, InMemoryMembersRepository(sessions), { today })
        val exercises = InMemoryExerciseRepository(path(0).steps.map { it.progress.exercise })
        val myGrowth = ObserveMyGrowthUseCase(
            ObserveCurrentMemberUseCase(sessions),
            ObserveSingerPathUseCase(exercises) { today },
            growth,
            objectives,
        )
        val ask = RequestLevelUseCase(sessions, myGrowth, growth)
        exercises.setStatus("me", "t1", ExerciseStatus.VALIDATED)
        exercises.setStatus("me", "t2", ExerciseStatus.VALIDATED)
        assertEquals(DomainResult.Failure(DomainError.INVALID_INPUT), ask())

        exercises.setStatus("me", "t3", ExerciseStatus.VALIDATED)
        assertEquals(DomainResult.Success(Unit), ask())
        val request = growth.observeRequests().first().single()
        assertEquals(Level.CONFIRMED, request.targetLevel)
        assertEquals(DomainResult.Failure(DomainError.INVALID_INPUT), ask(), "already waiting for an answer")

        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), ReviewLevelRequestUseCase(sessions, growth)(request.id, true, ""))
        val reviewer = FakeSessionRepository(member(Role.SUPER_ADMIN))
        assertEquals(DomainResult.Failure(DomainError.INVALID_INPUT), ReviewLevelRequestUseCase(reviewer, growth)(request.id, false, " "))
        ReviewLevelRequestUseCase(reviewer, growth)(request.id, false, "Encore un peu de justesse sur les aigus")
        assertEquals(ExerciseStatus.TO_REWORK, growth.observeRequests().first().single().status)
        assertEquals(DomainResult.Success(Unit), ask(), "can ask again after a send-back")
    }
}
