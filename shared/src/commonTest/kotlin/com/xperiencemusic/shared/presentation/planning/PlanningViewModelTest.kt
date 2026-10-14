package com.xperiencemusic.shared.presentation.planning

import app.cash.turbine.test
import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.data.repository.InMemoryPlanningRepository
import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObservePlanningUseCase
import com.xperiencemusic.shared.domain.usecase.OpenPlanningMonthUseCase
import com.xperiencemusic.shared.domain.usecase.ValidatePlanningMonthUseCase
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlanningViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `un admin ouvre le mois suivant puis le valide`() = runTest {
        val sessions = FakeSessionRepository(member(Role.CONTENT_ADMIN))
        val planning = InMemoryPlanningRepository(listOf(PlanningMonth(LocalDate(2026, 10, 1), null, true)))
        val viewModel = PlanningViewModel(
            ObservePlanningUseCase(planning, InMemoryMembersRepository(sessions), ObserveCurrentMemberUseCase(sessions)),
            GetTodayUseCase { LocalDate(2026, 10, 15) },
            OpenPlanningMonthUseCase(sessions, planning),
            ValidatePlanningMonthUseCase(sessions, planning),
        )
        viewModel.state.test {
            assertEquals(MonthStatus.VALIDATED, expectMostRecentItem().status)
            viewModel.onEvent(PlanningEvent.NextMonth)
            assertEquals(MonthStatus.NOT_OPEN, expectMostRecentItem().status)
            viewModel.onEvent(PlanningEvent.OpenMonthClicked)
            val opened = expectMostRecentItem()
            assertEquals(MonthStatus.OPEN, opened.status)
            assertEquals("25 oct.", opened.answerByLabel)
            viewModel.onEvent(PlanningEvent.ValidateMonthClicked)
            assertEquals(MonthStatus.VALIDATED, expectMostRecentItem().status)
        }
    }
}
