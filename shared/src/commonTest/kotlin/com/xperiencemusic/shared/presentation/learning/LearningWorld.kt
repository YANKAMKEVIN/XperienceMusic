package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.data.repository.InMemoryGrowthRepository
import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.data.seed.GrowthObjectives
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.usecase.GetLearningPreferencesUseCase
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.GradeActivityUseCase
import com.xperiencemusic.shared.domain.usecase.LoadLearningSessionUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveLearningProgressUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveObjectiveChecksUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveObjectiveRecordingsUseCase
import com.xperiencemusic.shared.domain.usecase.PlayLearningExampleUseCase
import com.xperiencemusic.shared.domain.usecase.PlayLocalAudioUseCase
import com.xperiencemusic.shared.domain.usecase.PlayReferenceNoteUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.RecordLearningAttemptUseCase
import com.xperiencemusic.shared.domain.usecase.SaveLearningPreferencesUseCase
import com.xperiencemusic.shared.domain.usecase.SaveLearningResumeUseCase
import com.xperiencemusic.shared.domain.usecase.SavePostponedActivitiesUseCase
import com.xperiencemusic.shared.domain.usecase.SyncLearningUseCase
import com.xperiencemusic.shared.domain.usecase.TickObjectiveUseCase
import com.xperiencemusic.shared.domain.usecase.TrackSingingUseCase
import com.xperiencemusic.shared.fakes.FakeAudioPlayer
import com.xperiencemusic.shared.fakes.FakeFileStore
import com.xperiencemusic.shared.fakes.FakeLearningRepository
import com.xperiencemusic.shared.fakes.FakeLearningSessionStore
import com.xperiencemusic.shared.fakes.FakeMicrophone
import com.xperiencemusic.shared.fakes.FakeObjectiveRecordingRepository
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.LocalDate

/** A member and their phone for the guided path: fakes behind every use case of [LearningViewModel]. */
class LearningWorld(
    scope: CoroutineScope,
    instrument: Instrument = Instrument.PIANO,
    today: LocalDate = LocalDate(2026, 10, 18),
    script: List<Int?> = emptyList(),
) {
    val sessions = FakeSessionRepository(member(instrument = instrument))
    val dates = DateProvider { today }
    val growth = InMemoryGrowthRepository(sessions, InMemoryMembersRepository(sessions), dates)
    val repository = FakeLearningRepository()
    val store = FakeLearningSessionStore()
    val recordings = FakeObjectiveRecordingRepository()
    val files = FakeFileStore()
    val player = FakeAudioPlayer()
    val playback = PlaybackController(player, { it }, scope)
    private val localAudio = PlayLocalAudioUseCase(files, player, playback)
    private val examples = PlayLearningExampleUseCase(localAudio, playback)
    private val microphone = FakeMicrophone(script)

    fun viewModel(moduleId: String = LearningCatalog.DEFAULT) = LearningViewModel(
        moduleId,
        ObserveCurrentMemberUseCase(sessions),
        LearningDependencies(
            observeProgress = ObserveLearningProgressUseCase(repository),
            syncLearning = SyncLearningUseCase(repository),
            gradeActivity = GradeActivityUseCase(RecordLearningAttemptUseCase(repository, dates)),
            observeChecks = ObserveObjectiveChecksUseCase(growth),
            observeRecordings = ObserveObjectiveRecordingsUseCase(recordings),
            requestReview = TickObjectiveUseCase(sessions, growth, GrowthObjectives.all),
            getPreferences = GetLearningPreferencesUseCase(store),
            savePreferences = SaveLearningPreferencesUseCase(store),
            loadSession = LoadLearningSessionUseCase(store),
            saveResume = SaveLearningResumeUseCase(store),
            savePostponed = SavePostponedActivitiesUseCase(store),
            today = GetTodayUseCase(dates),
            examples = examples,
            playback = playback,
            singing = SingingSessionController(TrackSingingUseCase(microphone), PlayReferenceNoteUseCase(localAudio), examples),
        ),
    )
}
