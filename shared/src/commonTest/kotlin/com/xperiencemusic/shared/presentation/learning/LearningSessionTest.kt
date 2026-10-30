package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.data.learning.FileLearningRepository
import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.repository.*
import com.xperiencemusic.shared.data.seed.GrowthObjectives
import com.xperiencemusic.shared.domain.learning.*
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.usecase.*
import com.xperiencemusic.shared.fakes.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LearningSessionTest {
    @BeforeTest fun setup()=Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun teardown()=Dispatchers.resetMain()
    @Test fun interruptionRestoresSelectionHintsAndPreferencesWithoutPenalty()=runTest {
        val sessions=FakeSessionRepository(member(instrument=Instrument.PIANO))
        val dates=DateProvider{LocalDate(2026,10,18)}
        val growth=InMemoryGrowthRepository(sessions,InMemoryMembersRepository(sessions),dates)
        val files=FakeFileStore();val snapshots=SnapshotStore(files)
        val repo=FileLearningRepository(snapshots)
        val player=FakeAudioPlayer();val playback=PlaybackController(player,{it},backgroundScope)
        fun create()=LearningViewModel(ObserveCurrentMemberUseCase(sessions),repo,growth,TickObjectiveUseCase(sessions,growth,GrowthObjectives.all),dates,PlayLearningExample(files,player,playback),snapshots)
        val first=create()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){first.state.collect()}
        first.onEvent(LearningEvent.Check);first.onEvent(LearningEvent.Next)
        first.onEvent(LearningEvent.Preferences(LearningPreferences(touchSound=false,rewardSound=false,haptics=false,weeklyTarget=3,startingPoint="piano-chart")))
        first.onEvent(LearningEvent.Key(60));first.onEvent(LearningEvent.Hint)
        first.onEvent(LearningEvent.Home)
        val next=create()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){next.state.collect()}
        next.onEvent(LearningEvent.Resume)
        assertEquals(1,next.state.value.index)
        assertEquals(setOf(60),next.state.value.notes)
        assertEquals(1,next.state.value.hints)
        assertFalse(next.state.value.preferences.touchSound)
        assertEquals(3,next.state.value.preferences.weeklyTarget)
        assertEquals("piano-chart",next.state.value.preferences.startingPoint)
        assertEquals(1,repo.observe("me").first().attempts.size)
        next.onEvent(LearningEvent.Key(64));next.onEvent(LearningEvent.Key(67));next.onEvent(LearningEvent.Check)
        assertEquals(true,next.state.value.result)
        assertEquals(1,repo.observe("me").first().attempts.last().hints)
        assertEquals(setOf(60,64,67),next.state.value.notes)
    }
}
