package com.xperiencemusic.shared.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.LearningModule
import com.xperiencemusic.shared.domain.model.SubmissionOutcome
import com.xperiencemusic.shared.domain.repository.NowPlaying
import com.xperiencemusic.shared.domain.usecase.PlayLocalAudioUseCase
import com.xperiencemusic.shared.domain.usecase.RecordedVoice
import com.xperiencemusic.shared.domain.usecase.RecordingLevel
import com.xperiencemusic.shared.domain.usecase.SubmitObjectiveRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.VoiceRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AssessmentError {
    /** The permission was refused: the screen offers the phone settings. */
    MICROPHONE_DENIED,
    MICROPHONE_UNAVAILABLE,

    /** The take stays on the phone, ready to be sent again. */
    SEND_FAILED,
}

data class AssessmentUiState(
    val module: LearningModule,
    val level: RecordingLevel = RecordingLevel(),
    val recordedSeconds: Int = 0,
    val sending: Boolean = false,
    val error: AssessmentError? = null,
    /** How the last sending ended, once the take reached the server. */
    val outcome: SubmissionOutcome? = null,
) {
    val microphoneDenied get() = error == AssessmentError.MICROPHONE_DENIED
}

/** Records a take of the module's objective and sends it to the responsable with an evaluation request. */
class AssessmentViewModel(
    moduleId: String,
    private val recorder: VoiceRecorder,
    private val preview: PlayLocalAudioUseCase,
    private val submit: SubmitObjectiveRecordingUseCase,
) : ViewModel() {
    private val module = LearningCatalog.module(moduleId)
    private val form = MutableStateFlow(AssessmentUiState(module))
    private var voice: RecordedVoice? = null

    val state: StateFlow<AssessmentUiState> = combine(form, recorder.level) { f, level -> f.copy(level = level) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AssessmentUiState(module))

    init {
        recorder.level
            .onEach {
                if (it.reachedLimit) stop()
                if (it.hasFailed) form.update { f -> f.copy(error = AssessmentError.MICROPHONE_UNAVAILABLE, outcome = null) }
            }
            .launchIn(viewModelScope)
    }

    fun start() {
        if (form.value.sending) return
        preview.stop()
        voice = null
        form.update { it.copy(recordedSeconds = 0, error = null, outcome = null) }
        recorder.start(viewModelScope)
    }

    fun denied() = form.update { it.copy(error = AssessmentError.MICROPHONE_DENIED, outcome = null) }

    fun stop() {
        voice = recorder.stop()
        form.update { it.copy(recordedSeconds = voice?.durationSeconds ?: 0) }
    }

    /** Plays the take back; the screen names it for the system controls. */
    fun listen(title: String) {
        voice?.let { preview("voice/objective-${module.objectiveId}.wav", it.wav, NowPlaying(title, module.title)) }
    }

    fun send() {
        val take = voice ?: return
        if (form.value.sending || take.durationSeconds < MIN_SECONDS) return
        form.update { it.copy(sending = true) }
        preview.stop()
        viewModelScope.launch {
            val result = submit(module, take.wav, take.durationSeconds)
            if (result is DomainResult.Success) voice = null
            form.update {
                when (result) {
                    is DomainResult.Success -> it.copy(sending = false, recordedSeconds = 0, error = null, outcome = result.value)
                    is DomainResult.Failure -> it.copy(sending = false, error = AssessmentError.SEND_FAILED, outcome = null)
                }
            }
        }
    }

    override fun onCleared() {
        recorder.discard()
        preview.stop()
    }

    private companion object {
        const val MIN_SECONDS = 3
    }
}
