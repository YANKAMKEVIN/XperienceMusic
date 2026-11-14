package com.xperiencemusic.shared.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.learning.challenge
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.GradeResult
import com.xperiencemusic.shared.domain.model.LearningProgress
import com.xperiencemusic.shared.domain.model.LearningResume
import com.xperiencemusic.shared.domain.model.ObjectiveCheck
import com.xperiencemusic.shared.domain.model.SingingProgress
import com.xperiencemusic.shared.domain.model.sessionPlan
import com.xperiencemusic.shared.domain.usecase.GetLearningPreferencesUseCase
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.GradeActivityUseCase
import com.xperiencemusic.shared.domain.usecase.LoadLearningSessionUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveLearningProgressUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveObjectiveChecksUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveObjectiveRecordingsUseCase
import com.xperiencemusic.shared.domain.usecase.PlayLearningExampleUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.SaveLearningPreferencesUseCase
import com.xperiencemusic.shared.domain.usecase.SaveLearningResumeUseCase
import com.xperiencemusic.shared.domain.usecase.SavePostponedActivitiesUseCase
import com.xperiencemusic.shared.domain.usecase.SyncLearningUseCase
import com.xperiencemusic.shared.domain.usecase.TickObjectiveUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

/** Everything the guided path needs besides the member, grouped to keep the constructor readable. */
class LearningDependencies(
    val observeProgress: ObserveLearningProgressUseCase,
    val syncLearning: SyncLearningUseCase,
    val gradeActivity: GradeActivityUseCase,
    val observeChecks: ObserveObjectiveChecksUseCase,
    val observeRecordings: ObserveObjectiveRecordingsUseCase,
    val requestReview: TickObjectiveUseCase,
    val getPreferences: GetLearningPreferencesUseCase,
    val savePreferences: SaveLearningPreferencesUseCase,
    val loadSession: LoadLearningSessionUseCase,
    val saveResume: SaveLearningResumeUseCase,
    val savePostponed: SavePostponedActivitiesUseCase,
    val today: GetTodayUseCase,
    val examples: PlayLearningExampleUseCase,
    val playback: PlaybackController,
    val singing: SingingSessionController,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LearningViewModel(
    moduleId: String,
    observeMember: ObserveCurrentMemberUseCase,
    dependencies: LearningDependencies,
) : ViewModel() {
    private val use = dependencies
    private val audio = dependencies.examples
    private val singing = dependencies.singing
    private val module = LearningCatalog.module(moduleId)
    private val form = MutableStateFlow(LearningForm())
    private val clock = TimeSource.Monotonic.markNow()
    private val member = observeMember().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val data = member.flatMapLatest { m ->
        if (m == null) {
            flowOf(LearningData(null, LearningProgress(), null, emptyList()))
        } else {
            val checks = use.observeChecks(m.id).map<List<ObjectiveCheck>, List<ObjectiveCheck>?> { it }.onStart { emit(null) }
            val takes = combine(use.observeRecordings(m.id), use.playback.state) { recordings, playback ->
                objectiveTakes(recordings, module.objectiveId, playback)
            }
            combine(use.observeProgress(m.id), checks, takes) { progress, c, t -> LearningData(m, progress, c, t) }
        }
    }

    val state: StateFlow<LearningUiState> = combine(data, form) { d, f -> LearningUiStateMapper.map(module, use.today(), d, f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearningUiState(module = module))

    init {
        member.filterNotNull()
            .onEach { m ->
                val preferences = use.getPreferences(m.id)
                val session = use.loadSession(m.id, module.id)
                form.update {
                    it.copy(
                        preferences = preferences,
                        postponed = it.postponed + session.postponed,
                        resumeIndex = it.resumeIndex ?: session.resume?.index,
                    )
                }
                use.syncLearning(m.id)
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: LearningEvent) {
        if (state.value.saving) return
        when (event) {
            is LearningEvent.Open -> open(event.index)
            LearningEvent.ChallengeExample -> audio.broken(module.challenge().example)
            LearningEvent.Begin -> begin()
            LearningEvent.Resume -> resume()
            LearningEvent.Home -> goHome()
            LearningEvent.Steps -> form.update { it.copy(steps = !it.steps) }
            LearningEvent.Settings -> form.update { it.copy(settings = !it.settings) }
            is LearningEvent.Preferences -> {
                form.update { it.copy(preferences = event.value) }
                state.value.memberId?.let { use.savePreferences(it, event.value) }
            }
            is LearningEvent.Key -> toggleKey(event.midi)
            is LearningEvent.Answer -> {
                form.update { it.copy(index = state.value.index, answer = event.value, result = null, feedback = null) }
                persist()
            }
            LearningEvent.Hint -> {
                form.update { it.copy(hints = (it.hints + 1).coerceAtMost(MAX_HINTS)) }
                persist()
            }
            LearningEvent.Listen -> {
                audio(state.value.activity.example)
                form.update { it.copy(index = state.value.index, listened = true) }
            }
            LearningEvent.ListenBroken -> {
                val s = state.value
                audio.broken(s.activity.example.ifEmpty { s.activity.expectedNotes.sorted() }.ifEmpty { s.singTargets })
                form.update { it.copy(listened = true) }
            }
            LearningEvent.ListenSelection -> audio(state.value.notes.sorted())
            LearningEvent.Metronome -> audio.metronome(state.value.activity.bpm)
            LearningEvent.Tap -> form.update {
                val now = clock.elapsedNow().inWholeMilliseconds / 1000.0
                it.copy(taps = (it.taps + now).takeLast(TAPS_KEPT), result = null)
            }
            LearningEvent.ResetTaps -> form.update { it.copy(taps = emptyList(), result = null, feedback = null) }
            LearningEvent.StartSinging -> startSinging()
            LearningEvent.StopSinging -> stopSinging()
            LearningEvent.MicrophoneDenied -> form.update { it.copy(micError = MicrophoneIssue.DENIED) }
            LearningEvent.Check -> check()
            LearningEvent.Postpone -> postpone()
            LearningEvent.Next -> next()
            LearningEvent.RequestReview -> requestReview()
            LearningEvent.Sync -> state.value.memberId?.let { viewModelScope.launch { use.syncLearning(it) } }
            is LearningEvent.PlayTake -> use.playback.playOrToggle(event.take.track(event.title, event.subtitle))
        }
    }

    private fun begin() {
        val s = state.value
        val plan = s.progress.sessionPlan(module, use.today(), s.postponed)
        form.update { it.copy(plan = plan, done = emptyList(), difficult = emptySet(), titles = emptyMap()) }
        open(plan.first())
    }

    private fun resume() {
        val id = state.value.memberId ?: return
        val saved = use.loadSession(id, module.id).resume
        if (saved != null && saved.index in module.activities.indices && saved.index <= state.value.reached) {
            form.update {
                it.copy(
                    index = saved.index,
                    encounter = saved.encounter,
                    notes = saved.notes,
                    answer = saved.answer,
                    hints = saved.hints,
                    mode = LearningMode.ACTIVITY,
                    result = null,
                    feedback = null,
                    taps = emptyList(),
                )
            }
        } else {
            open(state.value.reached)
        }
    }

    private fun goHome() {
        stopSinging()
        audio.stop()
        persist()
        form.update { it.copy(mode = LearningMode.HOME, taps = emptyList()) }
    }

    private fun toggleKey(midi: Int) {
        val s = state.value
        if (!s.allowed) return
        val selected = midi !in s.notes
        if (selected && s.preferences.touchSound) audio.key(midi)
        form.update {
            it.copy(index = s.index, notes = if (selected) it.notes + midi else it.notes - midi, result = null, feedback = null)
        }
        persist()
    }

    /** Remembers where the member is; after a success, the next step open to them. */
    private fun persist() {
        val id = state.value.memberId ?: return
        val f = form.value
        val resume = if (f.result == true) {
            val next = state.value.reached
            LearningResume(next, state.value.progress.attempts.count { it.activityId == module.activities[next].id })
        } else {
            LearningResume(f.index ?: state.value.index, f.encounter, f.notes, f.answer, f.hints)
        }
        use.saveResume(id, module.id, resume)
        form.update { it.copy(resumeIndex = resume.index) }
    }

    private fun open(index: Int) {
        if (index !in module.activities.indices || index > state.value.reached && !state.value.complete) return
        stopSinging()
        audio.stop()
        val encounter = state.value.progress.attempts.count { it.activityId == module.activities[index].id }
        form.update {
            it.copy(
                index = index,
                encounter = encounter,
                notes = emptySet(),
                answer = null,
                listened = false,
                result = null,
                hints = 0,
                mode = LearningMode.ACTIVITY,
                steps = false,
                feedback = null,
                taps = emptyList(),
                error = null,
                sung = false,
                heardMidi = null,
                singStep = 0,
                holdSeconds = 0.0,
                micError = null,
            )
        }
        persist()
    }

    private fun next() {
        if (state.value.activity.id !in state.value.completed) return
        advance()
    }

    private fun postpone() {
        val s = state.value
        if (!s.canPostpone) return
        val id = s.activity.id
        form.update {
            it.copy(index = s.index, postponed = it.postponed + id, difficult = it.difficult + id, titles = it.titles + (id to s.activity.title))
        }
        savePostponed()
        advance()
    }

    private fun savePostponed() {
        val id = state.value.memberId ?: return
        use.savePostponed(id, module.id, form.value.postponed)
    }

    /** To the next step of the session plan, or of the module, or to the summary. */
    private fun advance() {
        val f = form.value
        val index = state.value.index
        val planPosition = f.plan.indexOf(index)
        when {
            planPosition >= 0 && planPosition < f.plan.lastIndex -> open(f.plan[planPosition + 1])
            planPosition < 0 && index < module.activities.lastIndex -> open(index + 1)
            else -> {
                audio.stop()
                form.update { it.copy(mode = LearningMode.SUMMARY) }
            }
        }
    }

    private fun startSinging() {
        val s = state.value
        val target = s.activity.sing ?: return
        if (!s.allowed || singing.isListening) return
        form.update {
            it.copy(
                index = s.index,
                singing = true,
                sung = false,
                heardMidi = null,
                singStep = 0,
                holdSeconds = 0.0,
                micError = null,
                result = null,
                feedback = null,
                heardCents = null,
                micLevel = 0f,
                silentSeconds = 0.0,
            )
        }
        singing.start(
            scope = viewModelScope,
            sing = target,
            targets = s.singTargets,
            comfortMidi = s.comfortMidi,
            onProgress = { progress -> onSingingProgress(progress, target.semitones.size) },
            onFailure = { form.update { it.copy(singing = false, micError = MicrophoneIssue.UNAVAILABLE) } },
        )
    }

    private fun onSingingProgress(progress: SingingProgress, notes: Int) {
        form.update {
            it.copy(
                heardMidi = progress.heardMidi,
                singStep = progress.completedSteps,
                holdSeconds = progress.holdSeconds,
                heardCents = progress.centsFromTarget,
                micLevel = progress.level,
                silentSeconds = progress.silentSeconds,
            )
        }
        if (progress.isSuccess) {
            form.update { it.copy(singing = false, sung = true, singStep = notes) }
            stopSinging()
        }
    }

    private fun stopSinging() {
        singing.stop()
        form.update { if (it.singing) it.copy(singing = false) else it }
    }

    private fun check() {
        val s = state.value
        if (!s.canCheck) return
        val id = s.memberId ?: return
        val activity = s.activity
        form.update { it.copy(index = s.index, saving = true, error = null) }
        viewModelScope.launch {
            val graded = use.gradeActivity(id, activity, s.encounter, s.answerSoFar, s.hints)
            if (graded !is DomainResult.Success) {
                form.update { it.copy(saving = false, error = LearningError.SAVE_FAILED) }
                return@launch
            }
            val result = graded.value
            val passed = result.passed
            if (passed && activity.id in form.value.postponed) {
                form.update { it.copy(postponed = it.postponed - activity.id) }
                savePostponed()
            }
            form.update {
                it.copy(
                    saving = false,
                    result = passed,
                    feedback = CheckFeedback(result, s.hints),
                    done = if (passed) (it.done + activity.id).distinct() else it.done,
                    difficult = if (!passed || s.hints > 0) it.difficult + activity.id else it.difficult,
                    titles = it.titles + (activity.id to activity.title),
                    successSerial = it.successSerial + if (passed) 1 else 0,
                )
            }
            if (passed && s.preferences.rewardSound) audio.reward()
            persist()
            // A lesson is read, not answered: "J’ai compris" goes straight on, once the success is counted.
            if (result == GradeResult.LessonDone) {
                val counted = withTimeoutOrNull(LESSON_COUNT_TIMEOUT_MS) { state.first { activity.id in it.completed } }
                if (counted != null) advance()
            }
            use.syncLearning(id)
        }
    }

    private fun requestReview() {
        val s = state.value
        if (!s.allowed || !s.reviewLoaded || !s.complete) return
        if (s.reviewStatus in listOf(ExerciseStatus.SUBMITTED, ExerciseStatus.VALIDATED)) return
        form.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = use.requestReview(module.objectiveId, true)
            form.update { it.copy(saving = false, error = if (result is DomainResult.Failure) LearningError.REVIEW_NOT_SENT else null) }
        }
    }

    override fun onCleared() {
        singing.stop()
        audio.stop()
    }

    private companion object {
        const val MAX_HINTS = 3
        const val TAPS_KEPT = 8
        const val LESSON_COUNT_TIMEOUT_MS = 2_000L
    }
}
