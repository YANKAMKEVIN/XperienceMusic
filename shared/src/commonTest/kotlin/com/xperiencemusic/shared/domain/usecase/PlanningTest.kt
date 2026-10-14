package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.data.repository.InMemoryPlanningRepository
import com.xperiencemusic.shared.domain.model.BandMember
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.ServiceKind
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.planTeams
import com.xperiencemusic.shared.domain.model.servicesOf
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanningTest {
    private val november = LocalDate(2026, 11, 1)

    @Test
    fun `un mois compte ses vendredis et ses dimanches dans l'ordre`() {
        val days = servicesOf(november)
        assertEquals(listOf(1, 6, 8, 13, 15, 20, 22, 27, 29), days.map { it.day })
    }

    @Test
    fun `l'équipe suit l'ordre du groupe et signale les parties manquantes`() {
        val members = listOf(
            BandMember("p", "p@x", "Paul", Role.MEMBER, Instrument.DRUMS, null),
            BandMember("g", "g@x", "Grâce", Role.MEMBER, Instrument.SINGER, VoicePart.SOPRANO),
            BandMember("k", "k@x", "Kevin", Role.MEMBER, Instrument.SINGER, VoicePart.TENOR),
            BandMember("d", "d@x", "Daniel", Role.MEMBER, Instrument.PIANO, null),
        )
        val sunday = LocalDate(2026, 11, 1)
        val signups = listOf(
            ServiceSignup(sunday, "p", true),
            ServiceSignup(sunday, "k", true),
            ServiceSignup(sunday, "g", true),
            ServiceSignup(sunday, "d", false),
        )
        val team = planTeams(november, members, signups).first()
        assertEquals(ServiceKind.SUNDAY, team.kind)
        assertEquals(listOf("Grâce", "Kevin", "Paul"), team.serving.map { it.displayName })
        assertEquals(listOf(Instrument.PIANO, Instrument.BASS), team.missing)
    }

    @Test
    fun `un membre répond seulement pendant que le mois est ouvert`() = runTest {
        val planning = InMemoryPlanningRepository(listOf(PlanningMonth(november, null, isValidated = false)))
        val answer = AnswerServiceUseCase(FakeSessionRepository(member()), planning)
        assertEquals(DomainResult.Success(Unit), answer(LocalDate(2026, 11, 6), serves = true))
        assertEquals(listOf(ServiceSignup(LocalDate(2026, 11, 6), "me", true)), planning.observeSignups().first())

        planning.validateMonth(november)
        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), answer(LocalDate(2026, 11, 6), serves = false))
        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), answer(LocalDate(2026, 12, 4), serves = true))
    }

    @Test
    fun `seuls les vendredis et dimanches sont des services`() = runTest {
        val planning = InMemoryPlanningRepository(listOf(PlanningMonth(november, null, isValidated = false)))
        val answer = AnswerServiceUseCase(FakeSessionRepository(member()), planning)
        assertEquals(DomainResult.Failure(DomainError.INVALID_INPUT), answer(LocalDate(2026, 11, 3), serves = true))
    }

    @Test
    fun `les admins ouvrent valident et ajustent le planning`() = runTest {
        val planning = InMemoryPlanningRepository()
        val asMember = FakeSessionRepository(member())
        val asAdmin = FakeSessionRepository(member(Role.CONTENT_ADMIN))
        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), OpenPlanningMonthUseCase(asMember, planning)(november, null))
        assertEquals(
            DomainResult.Failure(DomainError.INVALID_INPUT),
            OpenPlanningMonthUseCase(asAdmin, planning)(november, LocalDate(2026, 11, 2)),
        )

        OpenPlanningMonthUseCase(asAdmin, planning)(november, LocalDate(2026, 10, 25))
        ValidatePlanningMonthUseCase(asAdmin, planning)(november)
        assertEquals(listOf(PlanningMonth(november, LocalDate(2026, 10, 25), true)), planning.observeMonths().first())

        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), AdjustServiceUseCase(asMember, planning)("x", november, true))
        AdjustServiceUseCase(asAdmin, planning)("x", november, true)
        assertEquals(listOf(ServiceSignup(november, "x", true)), planning.observeSignups().first())
    }
}
