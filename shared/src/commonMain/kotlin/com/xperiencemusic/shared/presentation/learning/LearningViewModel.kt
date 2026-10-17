package com.xperiencemusic.shared.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.learning.*
import com.xperiencemusic.shared.domain.model.*
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.GrowthRepository
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.TickObjectiveUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class LearningUiState(
    val loading: Boolean = true,
    val allowed: Boolean = false,
    val memberId: String? = null,
    val index: Int = 0,
    val progress: LearningProgress = LearningProgress(),
    val notes: Set<Int> = emptySet(),
    val answer: String? = null,
    val listened: Boolean = false,
    val result: Boolean? = null,
    val saving: Boolean = false,
    val error: String? = null,
    val reviewStatus: ExerciseStatus? = null,
    val feedback: String? = null,
    val reviewDue: Boolean = false,
) {
    val activity get() = PianoBasics.activities[index]
    val canCheck get() = !saving && allowed && when (activity.kind) {
        LearningKind.KEYBOARD -> notes.isNotEmpty()
        LearningKind.EAR -> listened && answer != null
        else -> true
    }
}

private data class LearningForm(
    val index: Int? = null,
    val notes: Set<Int> = emptySet(),
    val answer: String? = null,
    val listened: Boolean = false,
    val result: Boolean? = null,
    val saving: Boolean = false,
    val error: String? = null,
)

sealed interface LearningEvent {
    data class Open(val index: Int) : LearningEvent
    data class Key(val midi: Int) : LearningEvent
    data class Answer(val value: String) : LearningEvent
    data object Listen : LearningEvent
    data object ListenSelection : LearningEvent
    data object Check : LearningEvent
    data object Next : LearningEvent
    data object RequestReview : LearningEvent
    data object Sync : LearningEvent
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class LearningViewModel(
    observeMember: ObserveCurrentMemberUseCase,
    private val repository: LearningRepository,
    growth: GrowthRepository,
    private val tickObjective: TickObjectiveUseCase,
    private val dates: DateProvider,
    private val audio: PlayLearningExample,
) : ViewModel() {
    private val form = MutableStateFlow(LearningForm())
    private val member = observeMember().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val learning = member.flatMapLatest { m ->
        if (m == null) flowOf(LearningProgress()) else repository.observe(m.id)
    }
    private val review = member.flatMapLatest { m ->
        if (m == null) flowOf(emptyList()) else growth.observeChecks().onStart { emit(emptyList()) }
    }
    val state = combine(member, learning, review, form) { m, progress, checks, f ->
        val check = checks.firstOrNull { it.memberId == m?.id && it.objectiveId == PianoBasics.OBJECTIVE_ID }
        LearningUiState(
            loading = m == null, allowed = m?.profile?.instrument == Instrument.PIANO,
            memberId = m?.id, index = f.index ?: progress.nextIndex, progress = progress,
            notes = f.notes, answer = f.answer, listened = f.listened, result = f.result,
            saving = f.saving, error = f.error, reviewStatus = check?.status, feedback = check?.feedback,
            reviewDue = progress.reviewDue(dates.today()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearningUiState())

    init {
        member.filterNotNull().onEach { repository.sync(it.id) }.launchIn(viewModelScope)
    }

    fun onEvent(event: LearningEvent) {
        if (state.value.saving) return
        when (event) {
            is LearningEvent.Open -> open(event.index)
            LearningEvent.Next -> open((state.value.index + 1).coerceAtMost(PianoBasics.activities.lastIndex))
            is LearningEvent.Key -> {
                val current = state.value
                if (!current.allowed) return
                form.update { it.copy(index = current.index, notes = if (event.midi in it.notes) it.notes - event.midi else it.notes + event.midi, result = null, error = null) }
            }
            is LearningEvent.Answer -> form.update { it.copy(index = state.value.index, answer = event.value, result = null) }
            LearningEvent.Listen -> {
                audio(state.value.activity.example)
                form.update { it.copy(index = state.value.index, listened = true) }
            }
            LearningEvent.ListenSelection -> audio(state.value.notes.sorted())
            LearningEvent.Check -> check()
            LearningEvent.RequestReview -> requestReview()
            LearningEvent.Sync -> state.value.memberId?.let { id -> viewModelScope.launch { repository.sync(id) } }
        }
    }

    private fun open(index: Int) {
        if (index !in PianoBasics.activities.indices || index > state.value.progress.nextIndex && !state.value.progress.isComplete) return
        audio.stop()
        form.value = LearningForm(index = index)
    }

    private fun check() {
        val current = state.value
        if (!current.canCheck) return
        val id = current.memberId ?: return
        val passed = PianoBasics.check(current.activity, current.notes, current.answer)
        form.update { it.copy(index = current.index, saving = true, error = null) }
        viewModelScope.launch {
            try {
                repository.record(LearningAttempt(Uuid.random().toString(), id, current.activity.id, PianoBasics.VERSION, passed, dates.today()))
                form.update { it.copy(saving = false, result = passed) }
                repository.sync(id)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { form.update { it.copy(saving = false, error = "L’activité n’a pas pu être sauvegardée. Réessaie.") } }
        }
    }

    private fun requestReview() {
        val current = state.value
        if (!current.allowed || !current.progress.isComplete || current.reviewStatus == ExerciseStatus.SUBMITTED || current.reviewStatus == ExerciseStatus.VALIDATED) return
        form.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = tickObjective(PianoBasics.OBJECTIVE_ID, true)
            form.update { it.copy(saving = false, error = if (result is DomainResult.Failure) "Demande non envoyée. Vérifie ta connexion puis réessaie." else null) }
        }
    }

    override fun onCleared() { audio.stop() }
}
