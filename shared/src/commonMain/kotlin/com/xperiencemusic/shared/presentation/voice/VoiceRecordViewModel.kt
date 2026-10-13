package com.xperiencemusic.shared.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.PlaybackTrack
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import com.xperiencemusic.shared.domain.repository.NowPlaying
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.PlayLocalAudioUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.RecordedVoice
import com.xperiencemusic.shared.domain.usecase.SendVoiceUseCase
import com.xperiencemusic.shared.domain.usecase.VoiceRecorder
import com.xperiencemusic.shared.presentation.common.Formatting
import com.xperiencemusic.shared.presentation.common.shortLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the screen decided, as opposed to what is loaded. */
internal data class VoiceLocal(
    /** Null until the singer picks; then [NO_GUIDE] or a guide id. */
    val guideChoice: String? = null,
    val phase: VoicePhase = VoicePhase.READY,
    val error: VoiceError? = null,
)

/**
 * The singer records their part on [songId] with their voice track (or the reference) in the
 * earphones, listens back, then sends it to the super admin.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceRecordViewModel(
    private val songId: String,
    observeSong: ObserveSongUseCase,
    observeCurrentMember: ObserveCurrentMemberUseCase,
    submissions: VoiceSubmissionRepository,
    private val recorder: VoiceRecorder,
    private val sendVoice: SendVoiceUseCase,
    private val playback: PlaybackController,
    private val preview: PlayLocalAudioUseCase,
) : ViewModel() {
    private val local = MutableStateFlow(VoiceLocal())
    private var recorded: RecordedVoice? = null
    private var isPreviewLoaded = false
    private val song = observeSong(songId)
    private val member = observeCurrentMember()

    private val mine = member.flatMapLatest { m ->
        if (m == null) flowOf(null) else submissions.observeMine(m.id).map { list -> list.firstOrNull { it.songId == songId } }
    }

    val state: StateFlow<VoiceRecordUiState> = combine(song, member, mine, local, recorder.level) { song, member, mine, local, level ->
        deriveVoiceRecordState(song, member?.profile?.voicePart, mine, local, level.seconds, level.level)
    }.combine(preview.state) { state, player -> state.copy(isPreviewPlaying = isPreviewLoaded && player.isPlaying) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VoiceRecordUiState())

    init {
        // The length limit stops the recording by itself.
        recorder.level.onEach { if (it.reachedLimit && local.value.phase == VoicePhase.RECORDING) stop() }
            .launchIn(viewModelScope)
        recorder.level.onEach { if (it.hasFailed) failRecording() }.launchIn(viewModelScope)
    }

    fun onEvent(event: VoiceRecordEvent) {
        when (event) {
            is VoiceRecordEvent.GuideSelected -> local.update { it.copy(guideChoice = event.id ?: NO_GUIDE) }
            VoiceRecordEvent.RecordClicked -> start()
            VoiceRecordEvent.PermissionDenied -> local.update { it.copy(error = VoiceError.MICROPHONE) }
            VoiceRecordEvent.StopClicked -> stop()
            VoiceRecordEvent.PreviewClicked -> togglePreview()
            VoiceRecordEvent.RetakeClicked -> retake()
            VoiceRecordEvent.SendClicked -> send()
        }
    }

    private fun start() {
        if (local.value.phase == VoicePhase.RECORDING || local.value.phase == VoicePhase.SENDING) return
        stopPreview()
        recorded = null
        local.update { it.copy(phase = VoicePhase.RECORDING, error = null) }
        viewModelScope.launch {
            val current = state.value
            val guide = current.guides.firstOrNull { it.id == current.selectedGuideId }
            val loaded = song.first()
            if (guide != null && loaded != null) {
                val path = loaded.voiceTracks.firstOrNull { it.id == guide.id }?.audioPath
                    ?: loaded.recordings.firstOrNull { it.id == guide.id }?.audioPath
                path?.let { playback.playOrToggle(PlaybackTrack("guide-${guide.id}", guide.label, loaded.title, it)) }
            }
            recorder.start(viewModelScope)
        }
    }

    private fun stop() {
        if (local.value.phase != VoicePhase.RECORDING) return
        playback.stop()
        val voice = recorder.stop()
        if (voice == null || voice.durationSeconds < SendVoiceUseCase.MIN_SECONDS) {
            local.update { it.copy(phase = VoicePhase.READY, error = VoiceError.TOO_SHORT) }
            return
        }
        recorded = voice
        local.update { it.copy(phase = VoicePhase.RECORDED) }
    }

    private fun failRecording() {
        playback.stop()
        recorder.discard()
        local.update { it.copy(phase = VoicePhase.READY, error = VoiceError.MICROPHONE) }
    }

    private fun togglePreview() {
        val voice = recorded ?: return
        val player = preview.state.value
        when {
            !isPreviewLoaded -> {
                isPreviewLoaded = true
                preview("voice/$songId.wav", voice.wav, NowPlaying("Ma voix", state.value.songTitle))
            }
            player.isPlaying -> preview.pause()
            else -> preview.resume()
        }
    }

    private fun stopPreview() {
        if (isPreviewLoaded) preview.stop()
        isPreviewLoaded = false
    }

    private fun retake() {
        stopPreview()
        recorded = null
        recorder.discard()
        local.update { it.copy(phase = VoicePhase.READY, error = null) }
    }

    private fun send() {
        val voice = recorded ?: return
        stopPreview()
        local.update { it.copy(phase = VoicePhase.SENDING, error = null) }
        viewModelScope.launch {
            when (sendVoice(songId, voice.wav, voice.durationSeconds)) {
                is DomainResult.Success -> {
                    recorded = null
                    local.update { it.copy(phase = VoicePhase.SENT) }
                }
                is DomainResult.Failure -> local.update { it.copy(phase = VoicePhase.RECORDED, error = VoiceError.SEND_FAILED) }
            }
        }
    }

    override fun onCleared() {
        recorder.discard()
        stopPreview()
    }

    companion object {
        internal const val NO_GUIDE = "none"
    }
}

/** Pure: the screen from the song, the singer's voice, what they sent before and what is going on. */
internal fun deriveVoiceRecordState(
    song: Song?,
    voicePart: VoicePart?,
    previous: VoiceSubmission?,
    local: VoiceLocal,
    seconds: Double,
    level: Float,
): VoiceRecordUiState {
    if (song == null) return VoiceRecordUiState(isLoading = false)
    val myTrack = voicePart?.let { part -> song.voiceTracks.firstOrNull { it.voicePart == part } }
    val reference = song.referenceRecording
    val guides = listOfNotNull(
        myTrack?.let { GuideUi(it.id, it.voicePart.name, GuideKind.MY_VOICE, it.voicePart) },
        reference?.let { GuideUi(it.id, it.title, GuideKind.REFERENCE) },
    )
    val selected = when (local.guideChoice) {
        null -> guides.firstOrNull()?.id
        VoiceRecordViewModel.NO_GUIDE -> null
        else -> local.guideChoice
    }
    return VoiceRecordUiState(
        isLoading = false,
        songTitle = song.title,
        keyLabel = song.key.shortLabel(),
        voicePart = voicePart,
        guides = guides,
        selectedGuideId = selected,
        phase = local.phase,
        seconds = seconds.toInt(),
        maxSeconds = VoiceRecorder.MAX_SECONDS.toInt(),
        level = if (local.phase == VoicePhase.RECORDING) level else 0f,
        previous = previous?.let { PreviousSubmissionUi(Formatting.dayAndDate(it.submittedOn), it.status, it.feedback) },
        error = local.error,
    )
}

