package com.xperiencemusic.shared.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.shared.domain.model.DomainResult
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
import com.xperiencemusic.shared.domain.usecase.TrackHoldNoteUseCase
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
    private val trackHoldNote: TrackHoldNoteUseCase,
    private val completeExercise: CompleteExerciseUseCase,
    private val playReferenceNote: PlayReferenceNoteUseCase,
    private val playback: PlaybackController,
) : ViewModel() {
    private val _state = MutableStateFlow(ExerciseUiState())
    val state: StateFlow<ExerciseUiState> = _state.asStateFlow()

    private var listening: Job? = null

    init {
        observeCurrentMember()
            .filterNotNull()
            .flatMapLatest { member ->
                val voicePart = member.profile.voicePart ?: VoicePart.ALTO
                observeSingerPath(member.id, member.profile.level)
                    .map { path -> path.firstOrNull { it.exercise.id == exerciseId }?.let { it to voicePart } }
            }
            .filterNotNull()
            .onEach { (progress, voicePart) -> _state.update { it.withExercise(progress, voicePart) } }
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
        if (listening?.isActive == true) return
        // The microphone would hear the speakers otherwise.
        playback.stop()
        _state.update { it.copy(phase = ExercisePhase.LISTENING, error = null, check = check.reset()) }
        listening = trackHoldNote(check.targetMidi, check.requiredSeconds.toDouble())
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
        _state.update { it.copy(phase = ExercisePhase.SUCCEEDED, status = ExerciseStatus.VALIDATED) }
        viewModelScope.launch {
            if (completeExercise(exerciseId) is DomainResult.Failure) {
                _state.update { it.copy(error = ExerciseError.SAVE_FAILED) }
            }
        }
    }

    private fun stopListening() {
        listening?.cancel()
        _state.update { if (it.phase == ExercisePhase.LISTENING) it.copy(phase = ExercisePhase.READY) else it }
    }

    override fun onCleared() {
        listening?.cancel()
    }
}

private fun ExerciseUiState.withExercise(progress: ExerciseProgress, voicePart: VoicePart): ExerciseUiState {
    val exercise = progress.exercise
    val check = (exercise.check as? ExerciseCheck.HoldNote)?.let { hold ->
        val target = hold.targetMidi(voicePart)
        this.check?.takeIf { it.targetMidi == target }
            ?: AutomaticCheckUi(Pitch.noteLabel(target), target, hold.seconds.roundToInt())
    }
    return copy(
        isLoading = false,
        title = exercise.title,
        instructions = exercise.instructions,
        // A success seen on this screen wins over a status still being saved.
        status = if (status == ExerciseStatus.VALIDATED) status else progress.status,
        check = check,
    )
}

private fun AutomaticCheckUi.reset() = copy(heardNoteLabel = null, needle = null, tuning = Tuning.SILENT, holdSeconds = 0.0)

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
    )
}
