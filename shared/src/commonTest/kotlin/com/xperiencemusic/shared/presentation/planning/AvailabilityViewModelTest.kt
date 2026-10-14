package com.xperiencemusic.shared.presentation.planning

import app.cash.turbine.test
import com.xperiencemusic.shared.data.repository.InMemoryPlanningRepository
import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.usecase.AnswerServiceUseCase
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObservePlanningUseCase
import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
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
class AvailabilityViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `une réponse s'affiche tout de suite`() = runTest {
        val november = LocalDate(2026, 11, 1)
        val sessions = FakeSessionRepository(member(Role.MEMBER))
        val planning = InMemoryPlanningRepository(listOf(PlanningMonth(november, null, isValidated = false)))
        val viewModel = AvailabilityViewModel(
            november,
            ObservePlanningUseCase(planning, InMemoryMembersRepository(sessions), ObserveCurrentMemberUseCase(sessions)),
            GetTodayUseCase { LocalDate(2026, 10, 15) },
            AnswerServiceUseCase(sessions, planning),
        )
        viewModel.state.test {
            assertEquals(MyAnswer.UNANSWERED, expectMostRecentItem().rows.first().answer)
            viewModel.onEvent(AvailabilityEvent.Answered(november, serves = true))
            assertEquals(MyAnswer.SERVES, expectMostRecentItem().rows.first().answer)
        }
    }
}
