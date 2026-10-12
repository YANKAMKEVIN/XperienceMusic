package com.xperiencemusic.shared.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.StepState
import com.xperiencemusic.shared.domain.model.ExerciseCheck
import com.xperiencemusic.shared.domain.model.ExerciseProgress
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.SingingProgress
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.usecase.CompleteExerciseUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSingerPathUseCase
import com.xperiencemusic.shared.domain.usecase.PlayReferenceNoteUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.TrackSingingUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModel(
    private val exerciseId: String,
    observeCurrentMember: ObserveCurrentMemberUseCase,
    observeSingerPath: ObserveSingerPathUseCase,
    private val trackSinging: TrackSingingUseCase,
    private val completeExercise: CompleteExerciseUseCase,
    private val playReferenceNote: PlayReferenceNoteUseCase,
    private val playback: PlaybackController,
) : ViewModel() {
    private val _state = MutableStateFlow(ExerciseUiState())
    val state: StateFlow<ExerciseUiState> = _state.asStateFlow()

    private var listening: Job? = null
    private var exercise: Exercise? = null

    init {
        observeCurrentMember()
            .filterNotNull()
            .flatMapLatest { member ->
                val voicePart = member.profile.voicePart ?: VoicePart.ALTO
                observeSingerPath(member.id).map { path ->
                    val step = path.steps.firstOrNull { it.progress.exercise.id == exerciseId }
                    when {
                        step != null -> Triple(step.progress, step.state == StepState.LOCKED, voicePart)
                        path.warmUp?.id == exerciseId -> Triple(ExerciseProgress(path.warmUp, ExerciseStatus.TODO), false, voicePart)
                        else -> null
                    }
                }
            }
            .filterNotNull()
            .onEach { (progress, isLocked, voicePart) ->
                exercise = progress.exercise
                _state.update { it.withExercise(progress, isLocked, voicePart) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ExerciseEvent) {
        when (event) {
            ExerciseEvent.StartClicked -> startListening()
            ExerciseEvent.StopClicked -> stopListening()
            ExerciseEvent.PermissionDenied -> _state.update { it.copy(error = ExerciseError.MICROPHONE_DENIED) }
            ExerciseEvent.PlayReferenceClicked -> {
                stopListening()
                _state.value.check?.let { playReferenceNote(it.targetMidi) }
            }
        }
    }

    private fun startListening() {
        val check = _state.value.check ?: return
        if (listening?.isActive == true || _state.value.isLocked) return
        // The microphone would hear the speakers otherwise; only a harmony's other voice plays.
        playback.stop()
        check.accompanimentMidi?.let { playReferenceNote(it, seconds = (check.secondsEach + ACCOMPANIMENT_MARGIN_SECONDS).toInt()) }
        _state.update { it.copy(phase = ExercisePhase.LISTENING, error = null, check = check.reset()) }
        listening = trackSinging(check.targetMidis, check.secondsEach)
            .onEach { progress ->
                _state.update { it.copy(check = it.check?.withProgress(progress)) }
                if (progress.isSuccess) onSucceeded()
            }
            // Refused permission, microphone used by a call… all end the attempt the same way.
            .catch { _state.update { it.copy(phase = ExercisePhase.READY, error = ExerciseError.MICROPHONE_UNAVAILABLE) } }
            .launchIn(viewModelScope)
    }

    private fun onSucceeded() {
        listening?.cancel()
        playback.stop()
        _state.update {
            it.copy(phase = ExercisePhase.SUCCEEDED, status = if (it.isWarmUp) it.status else ExerciseStatus.VALIDATED)
        }
        val done = exercise ?: return
        viewModelScope.launch {
            if (completeExercise(done) is DomainResult.Failure) {
                _state.update { it.copy(error = ExerciseError.SAVE_FAILED) }
            }
        }
    }

    private fun stopListening() {
        if (listening?.isActive == true) playback.stop()
        listening?.cancel()
        _state.update { if (it.phase == ExercisePhase.LISTENING) it.copy(phase = ExercisePhase.READY) else it }
    }

    override fun onCleared() {
        listening?.cancel()
    }
}

private fun ExerciseUiState.withExercise(progress: ExerciseProgress, isLocked: Boolean, voicePart: VoicePart): ExerciseUiState {
    val exercise = progress.exercise
    val check = when (val c = exercise.check) {
        is ExerciseCheck.HoldNote -> {
            val target = c.targetMidi(voicePart)
            val accompaniment = c.accompanimentMidi(voicePart)
            AutomaticCheckUi(
                targetNoteLabel = Pitch.noteLabel(target),
                targetMidi = target,
                requiredSeconds = c.seconds.roundToInt(),
                accompanimentMidi = accompaniment,
                accompanimentLabel = accompaniment?.let(Pitch::noteLabel),
            )
        }
        is ExerciseCheck.NoteSequence -> {
            val targets = c.targetMidis(voicePart)
            AutomaticCheckUi(
                targetNoteLabel = Pitch.noteLabel(targets.first()),
                targetMidi = targets.first(),
                requiredSeconds = c.secondsEach.roundToInt().coerceAtLeast(1),
                targetMidis = targets,
                noteLabels = targets.map(Pitch::noteLabel),
                secondsEach = c.secondsEach,
            )
        }
        ExerciseCheck.Review -> null
    }
    return copy(
        isLoading = false,
        title = exercise.title,
        instructions = exercise.instructions,
        // A success seen on this screen wins over a status still being saved.
        status = if (status == ExerciseStatus.VALIDATED) status else progress.status,
        // Keep the live attempt when the path updates mid-way.
        check = this.check?.takeIf { it.targetMidis == check?.targetMidis } ?: check,
        isLocked = isLocked,
        isWarmUp = exercise.tier == Exercise.WARM_UP_TIER,
        isChallenge = exercise.isChallenge,
    )
}

private fun AutomaticCheckUi.reset() = copy(
    heardNoteLabel = null,
    needle = null,
    tuning = Tuning.SILENT,
    holdSeconds = 0.0,
    currentStep = 0,
    targetMidi = targetMidis.first(),
    targetNoteLabel = noteLabels.first(),
)

/** The other voice keeps playing a little after the hold, in case the singer starts late. */
private const val ACCOMPANIMENT_MARGIN_SECONDS = 20.0

/** The gauge spans one semitone each way; beyond, the needle is pinned to the edge. */
private const val NEEDLE_RANGE_CENTS = 100.0

/** Within this, the note counts as in tune (same tolerance as the hold check). */
private const val IN_TUNE_CENTS = 50.0

fun AutomaticCheckUi.withProgress(progress: SingingProgress): AutomaticCheckUi {
    val cents = progress.centsFromTarget
    return copy(
        heardNoteLabel = progress.heardMidi?.let(Pitch::noteLabel),
        needle = cents?.let { (it / NEEDLE_RANGE_CENTS).coerceIn(-1.0, 1.0).toFloat() },
        tuning = when {
            cents == null -> Tuning.SILENT
            abs(cents) <= IN_TUNE_CENTS -> Tuning.IN_TUNE
            cents < 0 -> Tuning.TOO_LOW
            else -> Tuning.TOO_HIGH
        },
        holdSeconds = progress.holdSeconds,
        currentStep = progress.stepIndex,
        targetMidi = targetMidis[progress.stepIndex.coerceIn(targetMidis.indices)],
        targetNoteLabel = noteLabels[progress.stepIndex.coerceIn(noteLabels.indices)],
    )
}
