package com.xperiencemusic.shared.presentation.voice

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.VoicePart

data class VoiceRecordUiState(
    val isLoading: Boolean = true,
    val songTitle: String = "",
    val keyLabel: String = "",
    /** Null when the singer gave no voice. */
    val voicePart: VoicePart? = null,
    /** What can play in the earphones while recording; null id = no guide. */
    val guides: List<GuideUi> = emptyList(),
    val selectedGuideId: String? = null,
    val phase: VoicePhase = VoicePhase.READY,
    val seconds: Int = 0,
    val maxSeconds: Int = 0,
    /** 0..1, how loud the voice is right now. */
    val level: Float = 0f,
    val isPreviewPlaying: Boolean = false,
    /** What was sent before for this song, and the answer if any. */
    val previous: PreviousSubmissionUi? = null,
    val error: VoiceError? = null,
)

/** [label] is the recording's title; a voice track is named after its [voicePart] on screen. */
data class GuideUi(val id: String, val label: String, val kind: GuideKind, val voicePart: VoicePart? = null)

enum class GuideKind { MY_VOICE, REFERENCE }

enum class VoicePhase { READY, RECORDING, RECORDED, SENDING, SENT }

enum class VoiceError { MICROPHONE, TOO_SHORT, SEND_FAILED }

data class PreviousSubmissionUi(val date: String, val status: ExerciseStatus, val feedback: String?)

sealed interface VoiceRecordEvent {
    data class GuideSelected(val id: String?) : VoiceRecordEvent
    /** Sent once the microphone permission is granted. */
    data object RecordClicked : VoiceRecordEvent
    data object PermissionDenied : VoiceRecordEvent
    data object StopClicked : VoiceRecordEvent
    data object PreviewClicked : VoiceRecordEvent
    data object RetakeClicked : VoiceRecordEvent
    data object SendClicked : VoiceRecordEvent
}
